package gr.grnet.keycloak.idp.mappers;

import org.keycloak.broker.saml.mappers.UserAttributeMapper;

import gr.grnet.keycloak.idp.EidasIdentityProviderFactory;

public class EidasUserAttributeMapper extends UserAttributeMapper {

	public static final String[] COMPATIBLE_PROVIDERS = { EidasIdentityProviderFactory.PROVIDER_ID };

	@Override
	public String[] getCompatibleProviders() {

		return COMPATIBLE_PROVIDERS;
	}

}
