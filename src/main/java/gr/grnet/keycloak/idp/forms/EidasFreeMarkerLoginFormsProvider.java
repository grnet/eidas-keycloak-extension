/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates and other 
 * contributors as indicated by the @author tags.
 * 
 * eIDAS modifications, Copyright 2021 GRNET, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gr.grnet.keycloak.idp.forms;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.forms.login.LoginFormsPages;
import org.keycloak.forms.login.MessageType;
import org.keycloak.forms.login.freemarker.AuthenticatorConfiguredMethod;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.forms.login.freemarker.model.ClientBean;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.forms.login.freemarker.model.LoginBean;
import org.keycloak.forms.login.freemarker.model.RealmBean;
import org.keycloak.forms.login.freemarker.model.RequiredActionUrlFormatterMethod;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.AdvancedMessageFormatterMethod;
import org.keycloak.theme.beans.LocaleBean;
import org.keycloak.theme.beans.MessageBean;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.theme.beans.MessagesPerFieldBean;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.StringUtil;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

public class EidasFreeMarkerLoginFormsProvider implements EidasLoginFormsProvider {

	private static final Logger logger = Logger.getLogger(EidasFreeMarkerLoginFormsProvider.class);

	public static final String EIDAS_SAML_POST_FORM = "eidas-saml-post-form.ftl";

	protected String accessCode;
	protected Response.Status status;
	protected List<AuthorizationDetails> clientScopesRequested;
	protected Map<String, String> httpResponseHeaders = new HashMap<>();
	protected URI actionUri;
	protected String execution;
	protected AuthenticationFlowContext context;

	protected List<FormMessage> messages = null;
	protected MessageType messageType = MessageType.ERROR;

	protected MultivaluedMap<String, String> formData;

	protected KeycloakSession session;
	/** authenticationSession can be null for some renderings, mainly error pages */
	protected AuthenticationSessionModel authenticationSession;
	protected RealmModel realm;
	protected ClientModel client;
	protected UriInfo uriInfo;

	protected FreeMarkerProvider freeMarker;
	protected final Map<String, Object> attributes = new HashMap<>();

	protected UserModel user;

	public EidasFreeMarkerLoginFormsProvider(KeycloakSession session) {
		this.session = session;
		this.freeMarker = session.getProvider(FreeMarkerProvider.class);
		this.attributes.put("scripts", new LinkedList<>());
		this.realm = session.getContext().getRealm();
		this.client = session.getContext().getClient();
		this.uriInfo = session.getContext().getUri();
	}

	@Override
	public Response createEidasSamlPostForm() {
		logger.info("Creating response for eidas saml");

		// custom eidas form
		Theme theme;
		try {
			theme = getTheme();
		} catch (IOException e) {
			logger.error("Failed to create theme", e);
			return Response.serverError().build();
		}

		Locale locale = session.getContext().resolveLocale(user);
		Properties messagesBundle = handleThemeResources(theme, locale);

		handleMessages(locale, messagesBundle);

		// for some reason Resteasy 2.3.7 doesn't like query params and form params with
		// the same name and will null out the code form param
		UriBuilder uriBuilder = prepareBaseUriBuilder(false);
		createCommonAttributes(theme, locale, messagesBundle, uriBuilder, null);

		attributes.put("login", new LoginBean(formData));
		if (status != null) {
			attributes.put("statusCode", status.getStatusCode());
		}

		attributes.put("samlPost", new EidasSAMLPostFormBean(formData));

		return processTemplate(theme, EIDAS_SAML_POST_FORM, locale);
	}

	/**
	 * Prepare base uri builder for later use
	 * 
	 * @param resetRequestUriParams - for some reason Resteasy 2.3.7 doesn't like
	 *                              query params and form params with the same name
	 *                              and will null out the code form param, so we
	 *                              have to reset them for some pages
	 * @return base uri builder
	 */
	protected UriBuilder prepareBaseUriBuilder(boolean resetRequestUriParams) {
		String requestURI = uriInfo.getBaseUri().getPath();
		UriBuilder uriBuilder = UriBuilder.fromUri(requestURI);
		if (resetRequestUriParams) {
			uriBuilder.replaceQuery(null);
		}

		if (client != null) {
			uriBuilder.queryParam(Constants.CLIENT_ID, client.getClientId());
		}
		if (authenticationSession != null) {
			uriBuilder.queryParam(Constants.TAB_ID, authenticationSession.getTabId());
		}
		return uriBuilder;
	}

	/**
	 * Get Theme used for page rendering.
	 * 
	 * @return theme for page rendering, never null
	 * @throws IOException in case of Theme loading problem
	 */
	protected Theme getTheme() throws IOException {
		return session.theme().getTheme(Theme.Type.LOGIN);
	}

	/**
	 * Load message bundle and place it into <code>msg</code> template attribute.
	 * Also load Theme properties and place them into <code>properties</code>
	 * template attribute.
	 * 
	 * @param theme  actual Theme to load bundle from
	 * @param locale to load bundle for
	 * @return message bundle for other use
	 */
	protected Properties handleThemeResources(Theme theme, Locale locale) {
		Properties messagesBundle = new Properties();
		try {
			if (!StringUtil.isNotBlank(realm.getDefaultLocale())) {
				messagesBundle.putAll(realm.getRealmLocalizationTextsByLocale(realm.getDefaultLocale()));
			}
			messagesBundle.putAll(theme.getMessages(locale));
			messagesBundle.putAll(realm.getRealmLocalizationTextsByLocale(locale.toLanguageTag()));
			attributes.put("msg", new MessageFormatterMethod(locale, messagesBundle));
			attributes.put("advancedMsg", new AdvancedMessageFormatterMethod(locale, messagesBundle));
		} catch (IOException e) {
			logger.warn("Failed to load messages", e);
			messagesBundle = new Properties();
		}

		try {
			attributes.put("properties", theme.getProperties());
		} catch (IOException e) {
			logger.warn("Failed to load properties", e);
		}

		return messagesBundle;
	}

	/**
	 * Handle messages to be shown on the page - set them to template attributes
	 * 
	 * @param locale         to be used for message text loading
	 * @param messagesBundle to be used for message text loading
	 * @see #messageType
	 * @see #messages
	 */
	protected void handleMessages(Locale locale, Properties messagesBundle) {
		MessagesPerFieldBean messagesPerField = new MessagesPerFieldBean();
		if (messages != null) {
			MessageBean wholeMessage = new MessageBean(null, messageType);
			for (FormMessage message : this.messages) {
				String formattedMessageText = formatMessage(message, messagesBundle, locale);
				if (formattedMessageText != null) {
					wholeMessage.appendSummaryLine(formattedMessageText);
					messagesPerField.addMessage(message.getField(), formattedMessageText, messageType);
				}
			}
			attributes.put("message", wholeMessage);
		} else {
			attributes.put("message", null);
		}
		attributes.put("messagesPerField", messagesPerField);
	}

	protected String formatMessage(FormMessage message, Properties messagesBundle, Locale locale) {
		if (message == null)
			return null;
		if (messagesBundle.containsKey(message.getMessage())) {
			return new MessageFormat(messagesBundle.getProperty(message.getMessage()), locale)
					.format(message.getParameters());
		} else {
			return message.getMessage();
		}
	}

	/**
	 * Create common attributes used in all templates.
	 * 
	 * @param theme          actual Theme used (provided by <code>getTheme()</code>)
	 * @param locale         actual locale
	 * @param messagesBundle actual message bundle (provided by
	 *                       <code>handleThemeResources()</code>)
	 * @param baseUriBuilder actual base uri builder (provided by
	 *                       <code>prepareBaseUriBuilder()</code>)
	 * @param page           in case if common page is rendered, is null if called
	 *                       from <code>createForm()</code>
	 * 
	 */
	protected void createCommonAttributes(Theme theme, Locale locale, Properties messagesBundle,
			UriBuilder baseUriBuilder, LoginFormsPages page) {
		URI baseUri = baseUriBuilder.build();
		if (accessCode != null) {
			baseUriBuilder.queryParam(LoginActionsService.SESSION_CODE, accessCode);
		}
		URI baseUriWithCodeAndClientId = baseUriBuilder.build();

		if (client != null) {
			attributes.put("client", new ClientBean(session, client));
		}

		if (realm != null) {
			attributes.put("realm", new RealmBean(realm));

			IdentityProviderBean idpBean = new IdentityProviderBean(session, realm, baseUriWithCodeAndClientId,
					context);

			attributes.put("social", idpBean);
			attributes.put("url", new UrlBean(realm, theme, baseUri, this.actionUri));
			attributes.put("requiredActionUrl", new RequiredActionUrlFormatterMethod(realm, baseUri));
			attributes.put("auth", new AuthenticationContextBean(context, page));
			attributes.put(Constants.EXECUTION, execution);

			if (realm.isInternationalizationEnabled()) {
				UriBuilder b;
				if (page != null) {
					switch (page) {
						case LOGIN:
						case LOGIN_USERNAME:
						case X509_CONFIRM:
							b = UriBuilder.fromUri(Urls.realmLoginPage(baseUri, realm.getName()));
							break;
						case REGISTER:
							b = UriBuilder.fromUri(Urls.realmRegisterPage(baseUri, realm.getName()));
							break;
						case LOGOUT_CONFIRM:
							b = UriBuilder.fromUri(Urls.logoutConfirm(baseUri, realm.getName()));
							break;
						default:
							b = UriBuilder.fromUri(baseUri).path(uriInfo.getPath());
							break;
					}
				} else {
					b = UriBuilder.fromUri(baseUri).path(uriInfo.getPath());
				}

				if (execution != null) {
					b.queryParam(Constants.EXECUTION, execution);
				}

				if (authenticationSession != null && authenticationSession.getAuthNote(Constants.KEY) != null) {
					b.queryParam(Constants.KEY, authenticationSession.getAuthNote(Constants.KEY));
				}

				attributes.put("locale", new LocaleBean(realm, locale, b, messagesBundle));
			}
		}
		if (realm != null && user != null && session != null) {
			attributes.put("authenticatorConfigured", new AuthenticatorConfiguredMethod(realm, user, session));
		}

		if (authenticationSession != null
				&& authenticationSession.getClientNote(Constants.KC_ACTION_EXECUTING) != null) {
			attributes.put("isAppInitiatedAction", true);
		}
	}

	/**
	 * Process FreeMarker template and prepare Response. Some fields are used for
	 * rendering also.
	 * 
	 * @param theme        to be used (provided by <code>getTheme()</code>)
	 * @param templateName name of the template to be rendered
	 * @param locale       to be used
	 * @return Response object to be returned to the browser, never null
	 */
	protected Response processTemplate(Theme theme, String templateName, Locale locale) {
		try {
			String result = freeMarker.processTemplate(attributes, templateName, theme);
			Response.ResponseBuilder builder = Response.status(status == null ? Response.Status.OK : status)
					.type(MediaType.TEXT_HTML_UTF_8_TYPE).language(locale).entity(result);
			for (Map.Entry<String, String> entry : httpResponseHeaders.entrySet()) {
				builder.header(entry.getKey(), entry.getValue());
			}
			return builder.build();
		} catch (FreeMarkerException e) {
			logger.error("Failed to process template", e);
			return Response.serverError().build();
		}
	}

	@Override
	public EidasFreeMarkerLoginFormsProvider setFormData(MultivaluedMap<String, String> formData) {
		this.formData = formData;
		return this;
	}

	@Override
	public void close() {
	}

}
