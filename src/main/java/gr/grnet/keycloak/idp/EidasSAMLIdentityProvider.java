package gr.grnet.keycloak.idp;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAML2NameIDPolicyBuilder;
import org.keycloak.saml.SAML2RequestedAuthnContextBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

public class EidasSAMLIdentityProvider extends SAMLIdentityProvider {

	private final EidasSAMLIdentityProviderConfig config;
	private final DestinationValidator destinationValidator;

	public EidasSAMLIdentityProvider(KeycloakSession session, EidasSAMLIdentityProviderConfig config,
			DestinationValidator destinationValidator) {
		super(session, config, destinationValidator);
		this.config = config;
		this.destinationValidator = destinationValidator;
	}

	@Override
	public EidasSAMLIdentityProviderConfig getConfig() {
		return this.config;
	}

	@Override
	public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
		logger.info("Creating EidasSAMLEndpoint");
		return new EidasSAMLEndpoint(realm, this, getConfig(), callback, destinationValidator);
	}

	@Override
	public Response performLogin(AuthenticationRequest request) {
		try {
			UriInfo uriInfo = request.getUriInfo();
			RealmModel realm = request.getRealm();
			String issuerURL = getEntityId(uriInfo, realm);
			String destinationUrl = getConfig().getSingleSignOnServiceUrl();
			String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();

			if (nameIDPolicyFormat == null) {
				nameIDPolicyFormat = JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
			}

			String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

			String assertionConsumerServiceUrl = request.getRedirectUri();

			if (getConfig().isPostBindingResponse()) {
				protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
			}

			SAML2RequestedAuthnContextBuilder requestedAuthnContext = new SAML2RequestedAuthnContextBuilder()
					.setComparison(getConfig().getAuthnContextComparisonType());

			requestedAuthnContext.addAuthnContextClassRef(getConfig().getLevelOfAssurance());

			for (String authnContextClassRef : getAuthnContextClassRefUris())
				requestedAuthnContext.addAuthnContextClassRef(authnContextClassRef);

			for (String authnContextDeclRef : getAuthnContextDeclRefUris())
				requestedAuthnContext.addAuthnContextDeclRef(authnContextDeclRef);

			Integer attributeConsumingServiceIndex = getConfig().getAttributeConsumingServiceIndex();

			String loginHint = getConfig().isLoginHint()
					? request.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM)
					: null;
			Boolean allowCreate = null;
			if (getConfig().getConfig().get(SAMLIdentityProviderConfig.ALLOW_CREATE) == null
					|| getConfig().isAllowCreate())
				allowCreate = Boolean.TRUE;
			SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
					.assertionConsumerUrl(assertionConsumerServiceUrl).destination(destinationUrl).issuer(issuerURL)
					.forceAuthn(getConfig().isForceAuthn()).protocolBinding(protocolBinding)
					.nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat).setAllowCreate(allowCreate))
					.attributeConsumingServiceIndex(attributeConsumingServiceIndex)
					.requestedAuthnContext(requestedAuthnContext).subject(loginHint);

			// eIDAS specific action, add the extensions
			authnRequestBuilder.addExtension(new EidasExtensionGenerator(getConfig()));

			JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session)
					.relayState(request.getState().getEncoded());
			boolean postBinding = getConfig().isPostBindingAuthnRequest();

			if (getConfig().isWantAuthnRequestsSigned()) {
				KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);

				String keyName = getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(),
						keys.getCertificate());
				binding.signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate())
						.signatureAlgorithm(getSignatureAlgorithm()).signDocument();
				if (!postBinding && getConfig().isAddExtensionsElementWithKeyInfo()) { // Only include extension if
																						// REDIRECT binding and signing
																						// whole SAML protocol message
					authnRequestBuilder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
				}
			}

			AuthnRequestType authnRequest = authnRequestBuilder.createAuthnRequest();

			for (Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils
					.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
				authnRequest = it.next().beforeSendingLoginRequest(authnRequest, request.getAuthenticationSession());
			}

			if (authnRequest.getDestination() != null) {
				destinationUrl = authnRequest.getDestination().toString();
			}

			// Save the current RequestID in the Auth Session as we need to verify it
			// against the ID returned from the IdP
			request.getAuthenticationSession().setClientNote(SamlProtocol.SAML_REQUEST_ID, authnRequest.getID());

			if (postBinding) {
				return binding.postBinding(authnRequestBuilder.toDocument()).request(destinationUrl);
			} else {
				return binding.redirectBinding(authnRequestBuilder.toDocument()).request(destinationUrl);
			}
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not create authentication request.", e);
		}
	}

	private List<String> getAuthnContextClassRefUris() {
		String authnContextClassRefs = getConfig().getAuthnContextClassRefs();
		if (authnContextClassRefs == null || authnContextClassRefs.isEmpty())
			return new LinkedList<String>();

		try {
			return Arrays.asList(JsonSerialization.readValue(authnContextClassRefs, String[].class));
		} catch (Exception e) {
			logger.warn("Could not json-deserialize AuthContextClassRefs config entry: " + authnContextClassRefs, e);
			return new LinkedList<String>();
		}
	}

	private List<String> getAuthnContextDeclRefUris() {
		String authnContextDeclRefs = getConfig().getAuthnContextDeclRefs();
		if (authnContextDeclRefs == null || authnContextDeclRefs.isEmpty())
			return new LinkedList<String>();

		try {
			return Arrays.asList(JsonSerialization.readValue(authnContextDeclRefs, String[].class));
		} catch (Exception e) {
			logger.warn("Could not json-deserialize AuthContextDeclRefs config entry: " + authnContextDeclRefs, e);
			return new LinkedList<String>();
		}
	}

	private String getEntityId(UriInfo uriInfo, RealmModel realm) {
		String configEntityId = getConfig().getEntityId();

		if (configEntityId == null || configEntityId.isEmpty())
			return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
		else
			return configEntityId;
	}

	@Override
	public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
		logger.info("Authentication finished");
		ResponseType responseType = (ResponseType) context.getContextData().get(SAMLEndpoint.SAML_LOGIN_RESPONSE);
		AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
		logger.info("Assertion=" + assertion);
		SubjectType subject = assertion.getSubject();
		SubjectType.STSubType subType = subject.getSubType();
		if (subType != null) {
			NameIDType subjectNameID = (NameIDType) subType.getBaseID();
			authSession.setUserSessionNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT_NAMEID,
					subjectNameID.serializeAsString());
		}
		AuthnStatementType authn = (AuthnStatementType) context.getContextData().get(SAMLEndpoint.SAML_AUTHN_STATEMENT);
		if (authn != null && authn.getSessionIndex() != null) {
			authSession.setUserSessionNote(SAMLEndpoint.SAML_FEDERATED_SESSION_INDEX, authn.getSessionIndex());
		}
	}

	@Override
	public IdentityProviderDataMarshaller getMarshaller() {
		return new EidasSAMLDataMarshaller();
	}

}
