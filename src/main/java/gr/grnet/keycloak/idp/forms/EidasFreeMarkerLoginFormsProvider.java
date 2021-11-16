package gr.grnet.keycloak.idp.forms;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.keycloak.forms.login.LoginFormsPages;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.LoginBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;

public class EidasFreeMarkerLoginFormsProvider extends FreeMarkerLoginFormsProvider {

	private static final Logger logger = Logger.getLogger(EidasFreeMarkerLoginFormsProvider.class);

	public static final String EIDAS_SAML_POST_FORM = "eidas-saml-post-form.ftl";

	public EidasFreeMarkerLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
		super(session, freeMarker);
	}

	protected Response createResponse(LoginFormsPages page) {
		if (LoginFormsPages.SAML_POST_FORM.equals(page)) {
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
			UriBuilder uriBuilder = prepareBaseUriBuilder(page == LoginFormsPages.OAUTH_GRANT);
			createCommonAttributes(theme, locale, messagesBundle, uriBuilder, page);

			attributes.put("login", new LoginBean(formData));
			if (status != null) {
				attributes.put("statusCode", status.getStatusCode());
			}

			attributes.put("samlPost", new EidasSAMLPostFormBean(formData));

			return processTemplate(theme, EIDAS_SAML_POST_FORM, locale);
		} else {
			// default behavior
			return super.createResponse(page);
		}
	}

}
