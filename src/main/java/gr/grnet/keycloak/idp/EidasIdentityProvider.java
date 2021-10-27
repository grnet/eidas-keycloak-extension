package gr.grnet.keycloak.idp;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.stream.XMLStreamWriter;

import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
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
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.util.JsonSerialization;


public class EidasIdentityProvider extends SAMLIdentityProvider {

    private final EidasIdentityProviderConfig config ;
    
    public EidasIdentityProvider(KeycloakSession session, EidasIdentityProviderConfig config, DestinationValidator destinationValidator) {
        super(session, config, destinationValidator);
        this.config = config;
    }
	
    @Override
    public EidasIdentityProviderConfig getConfig(){
        return this.config;
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
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }

            String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

            String assertionConsumerServiceUrl = request.getRedirectUri();

            if (getConfig().isPostBindingResponse()) {
                protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
            }

            // SAML2RequestedAuthnContextBuilder requestedAuthnContext =
            //     new SAML2RequestedAuthnContextBuilder()
            //         .setComparison(getConfig().getAuthnContextComparisonType());

            // for (String authnContextClassRef : getAuthnContextClassRefUris())
            //     requestedAuthnContext.addAuthnContextClassRef(authnContextClassRef);

            // for (String authnContextDeclRef : getAuthnContextDeclRefUris())
            //     requestedAuthnContext.addAuthnContextDeclRef(authnContextDeclRef);

			 SAML2RequestedAuthnContextBuilder requestedAuthnContext =new SAML2RequestedAuthnContextBuilder()
                    .setComparison(getConfig().getAuthnContextComparisonType());

			requestedAuthnContext.addAuthnContextClassRef(getConfig().getLevelOfAssurance());

			Integer attributeConsumingServiceIndex = getConfig().getAttributeConsumingServiceIndex();

            String loginHint = getConfig().isLoginHint() ? request.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM) : null;
            Boolean allowCreate = null;
            if (getConfig().getConfig().get(SAMLIdentityProviderConfig.ALLOW_CREATE) == null || getConfig().isAllowCreate())
                allowCreate = Boolean.TRUE;
            SAML2AuthnRequestBuilder authnRequestBuilder = new EidasSAML2AuthnRequestBuilder()
                    .assertionConsumerUrl(assertionConsumerServiceUrl)
                    .destination(destinationUrl)
                    .issuer(issuerURL)
                    .forceAuthn(getConfig().isForceAuthn())
                    .protocolBinding(protocolBinding)
                    .nameIdPolicy(SAML2NameIDPolicyBuilder
                        .format(nameIDPolicyFormat)
                        .setAllowCreate(allowCreate))
                    .attributeConsumingServiceIndex(attributeConsumingServiceIndex)
                    .requestedAuthnContext(requestedAuthnContext)
                    .subject(loginHint);
            
			authnRequestBuilder.addExtension(new EidasExtensionGenerator());


            JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session)
                    .relayState(request.getState().getEncoded());
            boolean postBinding = getConfig().isPostBindingAuthnRequest();

            if (getConfig().isWantAuthnRequestsSigned()) {
                KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);

                String keyName = getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
                binding.signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate())
                        .signatureAlgorithm(getSignatureAlgorithm())
                        .signDocument();
                if (! postBinding && getConfig().isAddExtensionsElementWithKeyInfo()) {    // Only include extension if REDIRECT binding and signing whole SAML protocol message
                    authnRequestBuilder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
                }
            }

            AuthnRequestType authnRequest = authnRequestBuilder.createAuthnRequest();
			
            for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext(); ) {
                authnRequest = it.next().beforeSendingLoginRequest(authnRequest, request.getAuthenticationSession());
            }

            if (authnRequest.getDestination() != null) {
                destinationUrl = authnRequest.getDestination().toString();
            }

            // Save the current RequestID in the Auth Session as we need to verify it against the ID returned from the IdP
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
	
	private static class EidasExtensionGenerator implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

		@Override
		public void write(XMLStreamWriter writer) throws ProcessingException {
			final String EIDAS_NS_URI = "http://eidas.europa.eu/saml-extensions"; 
			StaxUtil.writeNameSpace(writer, "eidas", EIDAS_NS_URI);

			StaxUtil.writeStartElement(writer, "eidas", "SPType", EIDAS_NS_URI);
			StaxUtil.writeCData(writer, "public");
			StaxUtil.writeEndElement(writer);

			StaxUtil.writeStartElement(writer, "eidas", "RequestedAttributes", EIDAS_NS_URI);
			
			StaxUtil.writeStartElement(writer, "eidas", "RequestedAttribute", EIDAS_NS_URI);
			StaxUtil.writeAttribute(writer, "Name", "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier");
			StaxUtil.writeAttribute(writer, "NameFormat", "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
			StaxUtil.writeAttribute(writer, "isRequired", "true");
			StaxUtil.writeEndElement(writer);

			StaxUtil.writeStartElement(writer, "eidas", "RequestedAttribute", EIDAS_NS_URI);
			StaxUtil.writeAttribute(writer, "Name", "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName");
			StaxUtil.writeAttribute(writer, "NameFormat", "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
			StaxUtil.writeAttribute(writer, "isRequired", "true");
			StaxUtil.writeEndElement(writer);

			StaxUtil.writeStartElement(writer, "eidas", "RequestedAttribute", EIDAS_NS_URI);
			StaxUtil.writeAttribute(writer, "Name", "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName");
			StaxUtil.writeAttribute(writer, "NameFormat", "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
			StaxUtil.writeAttribute(writer, "isRequired", "true");
			StaxUtil.writeEndElement(writer);

			StaxUtil.writeStartElement(writer, "eidas", "RequestedAttribute", EIDAS_NS_URI);
			StaxUtil.writeAttribute(writer, "Name", "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth");
			StaxUtil.writeAttribute(writer, "NameFormat", "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
			StaxUtil.writeAttribute(writer, "isRequired", "true");
			StaxUtil.writeEndElement(writer);
			
			StaxUtil.writeEndElement(writer);

			StaxUtil.flush(writer);
		}
	}
}

