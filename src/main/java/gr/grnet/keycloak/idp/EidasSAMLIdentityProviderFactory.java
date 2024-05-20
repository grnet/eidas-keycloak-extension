/*
 * Copyright 2021 GRNET, Inc.
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
package gr.grnet.keycloak.idp;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.keycloak.Config.Scope;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.util.SAMLMetadataUtil;
import org.keycloak.saml.validators.DestinationValidator;
import org.w3c.dom.Element;

public class EidasSAMLIdentityProviderFactory implements IdentityProviderFactory<EidasSAMLIdentityProvider> {

	public static final String PROVIDER_ID = "eidas-saml";

    private static final String MACEDIR_ENTITY_CATEGORY = "http://macedir.org/entity-category";
    private static final String REFEDS_HIDE_FROM_DISCOVERY = "http://refeds.org/category/hide-from-discovery";

	private DestinationValidator destinationValidator;

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getName() {
		return "eIDAS SAML v2.0";
	}

	@Override
	public EidasSAMLIdentityProviderConfig createConfig() {
		return new EidasSAMLIdentityProviderConfig();
	}

	@Override
	public EidasSAMLIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
		return new EidasSAMLIdentityProvider(session, new EidasSAMLIdentityProviderConfig(model), destinationValidator);
	}

	@Override
	public EidasSAMLIdentityProvider create(KeycloakSession session) {
		return null;
	}

	@Override
	public void init(Scope config) {
		this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return ProviderConfigurationBuilder.create()
				.property()
				.name(EidasSAMLIdentityProviderConfig.SERVICE_PROVIDER_COUNTRY_OF_ORIGIN)
				.type(ProviderConfigProperty.STRING_TYPE)
				.label("identity-provider.eidas.saml.service-provider-country-of-origin")
				.helpText("identity-provider.eidas.saml.service-provider-country-of-origin.tooltip")
			.add()			
				.property()
				.name(EidasSAMLIdentityProviderConfig.LEVEL_OF_ASSURANCE)
				.type(ProviderConfigProperty.STRING_TYPE)
				.label("identity-provider.eidas.saml.level-of-assurance")
				.helpText("identity-provider.eidas.saml.level-of-assurance.tooltip")
			.add()
				.property()
				.name(EidasSAMLIdentityProviderConfig.PRIVATE_SERVICE_PROVIDER)
				.type(ProviderConfigProperty.BOOLEAN_TYPE)
				.label("identity-provider.eidas.saml.private-service-provider")
				.helpText("identity-provider.eidas.saml.private-service-provider.tooltip")
			.add()								
				.property()
				.name(EidasSAMLIdentityProviderConfig.REQUESTED_ATTRIBUTES)
				.type(ProviderConfigProperty.STRING_TYPE)
				.label("identity-provider.eidas.saml.requested-attributes")
				.helpText("identity-provider.eidas.saml.requested-attributes.tooltip")
			.add()
		.build();
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, InputStream inputStream) {
        try {
            EntityDescriptorType entityType = SAMLMetadataUtil.parseEntityDescriptorType(inputStream);
            IDPSSODescriptorType idpDescriptor = SAMLMetadataUtil.locateIDPSSODescriptorType(entityType);

            if (idpDescriptor != null) {
                SAMLIdentityProviderConfig samlIdentityProviderConfig = new SAMLIdentityProviderConfig();
                String singleSignOnServiceUrl = null;
                boolean postBindingResponse = false;
                boolean postBindingLogout = false;
                for (EndpointType endpoint : idpDescriptor.getSingleSignOnService()) {
                    if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                        singleSignOnServiceUrl = endpoint.getLocation().toString();
                        postBindingResponse = true;
                        break;
                    } else if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
                        singleSignOnServiceUrl = endpoint.getLocation().toString();
                    }
                }
                String singleLogoutServiceUrl = null;
                for (EndpointType endpoint : idpDescriptor.getSingleLogoutService()) {
                    if (postBindingResponse && endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                        singleLogoutServiceUrl = endpoint.getLocation().toString();
                        postBindingLogout = true;
                        break;
                    } else if (!postBindingResponse && endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
                        singleLogoutServiceUrl = endpoint.getLocation().toString();
                        break;
                    }

                }
                samlIdentityProviderConfig.setIdpEntityId(entityType.getEntityID());
                samlIdentityProviderConfig.setSingleLogoutServiceUrl(singleLogoutServiceUrl);
                samlIdentityProviderConfig.setSingleSignOnServiceUrl(singleSignOnServiceUrl);
                samlIdentityProviderConfig.setWantAuthnRequestsSigned(idpDescriptor.isWantAuthnRequestsSigned());
                samlIdentityProviderConfig.setAddExtensionsElementWithKeyInfo(false);
                samlIdentityProviderConfig.setValidateSignature(idpDescriptor.isWantAuthnRequestsSigned());
                samlIdentityProviderConfig.setPostBindingResponse(postBindingResponse);
                samlIdentityProviderConfig.setPostBindingAuthnRequest(postBindingResponse);
                samlIdentityProviderConfig.setPostBindingLogout(postBindingLogout);
                samlIdentityProviderConfig.setLoginHint(false);

                List<String> nameIdFormatList = idpDescriptor.getNameIDFormat();
                if (nameIdFormatList != null && !nameIdFormatList.isEmpty()) {
                    samlIdentityProviderConfig.setNameIDPolicyFormat(nameIdFormatList.get(0));
                }

                List<KeyDescriptorType> keyDescriptor = idpDescriptor.getKeyDescriptor();
                String defaultCertificate = null;

                if (keyDescriptor != null) {
                    for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
                        Element keyInfo = keyDescriptorType.getKeyInfo();
                        Element x509KeyInfo = DocumentUtil.getChildElement(keyInfo, new QName("dsig", "X509Certificate"));

                        if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
                            samlIdentityProviderConfig.addSigningCertificate(x509KeyInfo.getTextContent());
                        } else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
                            samlIdentityProviderConfig.setEncryptionPublicKey(x509KeyInfo.getTextContent());
                        } else if (keyDescriptorType.getUse() == null) {
                            defaultCertificate = x509KeyInfo.getTextContent();
                        }
                    }
                }

                if (defaultCertificate != null) {
                    if (samlIdentityProviderConfig.getSigningCertificates().length == 0) {
                        samlIdentityProviderConfig.addSigningCertificate(defaultCertificate);
                    }

                    if (samlIdentityProviderConfig.getEncryptionPublicKey() == null) {
                        samlIdentityProviderConfig.setEncryptionPublicKey(defaultCertificate);
                    }
                }

                samlIdentityProviderConfig.setEnabledFromMetadata(entityType.getValidUntil() == null
                        || entityType.getValidUntil().toGregorianCalendar().getTime().after(new Date(System.currentTimeMillis())));

                // check for hide on login attribute
                if (entityType.getExtensions() != null && entityType.getExtensions().getEntityAttributes() != null) {
                    for (AttributeType attribute : entityType.getExtensions().getEntityAttributes().getAttribute()) {
                        if (MACEDIR_ENTITY_CATEGORY.equals(attribute.getName())
                                && attribute.getAttributeValue().contains(REFEDS_HIDE_FROM_DISCOVERY)) {
                            samlIdentityProviderConfig.setHideOnLogin(true);
                        }
                    }

                }

                return samlIdentityProviderConfig.getConfig();
            }
        } catch (ParsingException pe) {
            throw new RuntimeException("Could not parse IdP SAML Metadata", pe);
        }

        return new HashMap<>();
    }

}
