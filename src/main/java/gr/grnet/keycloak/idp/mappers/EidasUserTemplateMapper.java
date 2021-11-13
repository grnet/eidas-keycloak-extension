package gr.grnet.keycloak.idp.mappers;

import org.keycloak.broker.saml.mappers.UsernameTemplateMapper;

import gr.grnet.keycloak.idp.EidasSAMLIdentityProviderFactory;

public class EidasUserTemplateMapper extends UsernameTemplateMapper {

    public static final String[] COMPATIBLE_PROVIDERS = { EidasSAMLIdentityProviderFactory.PROVIDER_ID };

    public static final String PROVIDER_ID = "eidas-saml-username-idp-mapper";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }
	
}
