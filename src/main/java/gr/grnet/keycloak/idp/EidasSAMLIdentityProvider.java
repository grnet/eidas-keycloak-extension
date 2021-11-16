package gr.grnet.keycloak.idp;

import java.io.StringWriter;
import java.net.URI;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamWriter;

import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.ExtensionsType;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.protocol.saml.mappers.SamlMetadataDescriptorUpdater;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAML2NameIDPolicyBuilder;
import org.keycloak.saml.SAML2RequestedAuthnContextBuilder;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import gr.grnet.keycloak.idp.forms.CitizenCountrySelectorAuthenticatorForm;

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
			
			// eIDAS specific action, try to figure out citizen's country. This should be set using a CitizenCountrySelectorAuthenticatorForm
			// in the login flow.
			String country = request.getAuthenticationSession().getAuthNote(CitizenCountrySelectorAuthenticatorForm.CITIZEN_COUNTRY);
			logger.info("Citizen Country selected=" + country);

			EidasJaxrsSAML2BindingBuilder binding = new EidasJaxrsSAML2BindingBuilder(session)
					.relayState(request.getState().getEncoded())
					.country(country);
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

	@Override
	public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
		ResponseType responseType = (ResponseType) context.getContextData().get(SAMLEndpoint.SAML_LOGIN_RESPONSE);
		AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
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

	@Override
	public Response export(UriInfo uriInfo, RealmModel realm, String format) {
		try {
			URI authnBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();

			if (getConfig().isPostBindingAuthnRequest()) {
				authnBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
			}

			URI endpoint = uriInfo.getBaseUriBuilder().path("realms").path(realm.getName()).path("broker")
					.path(getConfig().getAlias()).path("endpoint").build();

			boolean wantAuthnRequestsSigned = getConfig().isWantAuthnRequestsSigned();
			boolean wantAssertionsSigned = getConfig().isWantAssertionsSigned();
			boolean wantAssertionsEncrypted = getConfig().isWantAssertionsEncrypted();
			String entityId = getEntityId(uriInfo, realm);
			String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();
			int attributeConsumingServiceIndex = getConfig().getAttributeConsumingServiceIndex() != null
					? getConfig().getAttributeConsumingServiceIndex()
					: 1;
			String attributeConsumingServiceName = getConfig().getAttributeConsumingServiceName();

			List<Element> signingKeys = new LinkedList<>();
			List<Element> encryptionKeys = new LinkedList<>();

			session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256).filter(Objects::nonNull)
					.filter(key -> key.getCertificate() != null).sorted(SamlService::compareKeys).forEach(key -> {
						try {
							Element element = SPMetadataDescriptor.buildKeyInfoElement(key.getKid(),
									PemUtils.encodeCertificate(key.getCertificate()));
							signingKeys.add(element);

							if (key.getStatus() == KeyStatus.ACTIVE) {
								encryptionKeys.add(element);
							}
						} catch (ParserConfigurationException e) {
							logger.warn("Failed to export SAML SP Metadata!", e);
							throw new RuntimeException(e);
						}
					});

			// Prepare the metadata descriptor model
			StringWriter sw = new StringWriter();
			XMLStreamWriter writer = StaxUtil.getXMLStreamWriter(sw);
			SAMLMetadataWriter metadataWriter = new EidasSAMLMetadataWriter(writer);

			EntityDescriptorType entityDescriptor = SPMetadataDescriptor.buildSPdescriptor(authnBinding, authnBinding,
					endpoint, endpoint, wantAuthnRequestsSigned, wantAssertionsSigned, wantAssertionsEncrypted,
					entityId, nameIDPolicyFormat, signingKeys, encryptionKeys);

			// Create the AttributeConsumingService
			AttributeConsumingServiceType attributeConsumingService = new AttributeConsumingServiceType(
					attributeConsumingServiceIndex);
			attributeConsumingService.setIsDefault(true);

			if (attributeConsumingServiceName != null && attributeConsumingServiceName.length() > 0) {
				String currentLocale = realm.getDefaultLocale() == null ? "en" : realm.getDefaultLocale();
				LocalizedNameType attributeConsumingServiceNameElement = new LocalizedNameType(currentLocale);
				attributeConsumingServiceNameElement.setValue(attributeConsumingServiceName);
				attributeConsumingService.addServiceName(attributeConsumingServiceNameElement);
			}

			// If node country is defined, look for the SP description and add it
			String nodeCountry = getConfig().getServiceProviderCountryOfOrigin();
			if (!StringUtil.isNullOrEmpty(nodeCountry)) {
				for (EntityDescriptorType.EDTChoiceType choiceType : entityDescriptor.getChoiceType()) {
					List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();

					if (descriptors != null) {
						for (EntityDescriptorType.EDTDescriptorChoiceType descriptor : descriptors) {
							SPSSODescriptorType spDescriptor = descriptor.getSpDescriptor();
							if (spDescriptor != null) {
								updateExtensionsType(spDescriptor, nodeCountry);
							}
						}
					}
				}
			}

			// Look for the SP descriptor and add the attribute consuming service
			for (EntityDescriptorType.EDTChoiceType choiceType : entityDescriptor.getChoiceType()) {
				List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();

				if (descriptors != null) {
					for (EntityDescriptorType.EDTDescriptorChoiceType descriptor : descriptors) {
						if (descriptor.getSpDescriptor() != null) {
							descriptor.getSpDescriptor().addAttributeConsumerService(attributeConsumingService);
						}
					}
				}
			}

			// Add the attribute mappers
			realm.getIdentityProviderMappersByAliasStream(getConfig().getAlias()).forEach(mapper -> {
				IdentityProviderMapper target = (IdentityProviderMapper) session.getKeycloakSessionFactory()
						.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
				if (target instanceof SamlMetadataDescriptorUpdater) {
					SamlMetadataDescriptorUpdater metadataAttrProvider = (SamlMetadataDescriptorUpdater) target;
					metadataAttrProvider.updateMetadata(mapper, entityDescriptor);
				}
			});

			// Write the metadata and export it to a string
			metadataWriter.writeEntityDescriptor(entityDescriptor);

			String descriptor = sw.toString();

			// Metadata signing
			if (getConfig().isSignSpMetadata()) {
				KeyManager.ActiveRsaKey activeKey = session.keys().getActiveRsaKey(realm);
				String keyName = getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(activeKey.getKid(),
						activeKey.getCertificate());
				KeyPair keyPair = new KeyPair(activeKey.getPublicKey(), activeKey.getPrivateKey());

				Document metadataDocument = DocumentUtil.getDocument(descriptor);
				SAML2Signature signatureHelper = new SAML2Signature();
				signatureHelper.setSignatureMethod(getSignatureAlgorithm().getXmlSignatureMethod());
				signatureHelper.setDigestMethod(getSignatureAlgorithm().getXmlSignatureDigestMethod());

				Node nextSibling = metadataDocument.getDocumentElement().getFirstChild();
				signatureHelper.setNextSibling(nextSibling);

				signatureHelper.signSAMLDocument(metadataDocument, keyName, keyPair, CanonicalizationMethod.EXCLUSIVE);

				descriptor = DocumentUtil.getDocumentAsString(metadataDocument);
			}

			return Response.ok(descriptor, MediaType.APPLICATION_XML_TYPE).build();
		} catch (Exception e) {
			logger.warn("Failed to export SAML SP Metadata!", e);
			throw new RuntimeException(e);
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

	private void updateExtensionsType(SPSSODescriptorType spDescriptor, String nodeCountry) {
		ExtensionsType extensions = spDescriptor.getExtensions();
		if (extensions == null) {
			extensions = new ExtensionsType();
			spDescriptor.setExtensions(extensions);
		}
		extensions.addExtension(new EidasNodeCountryExtensionGenerator(nodeCountry));
	}

}
