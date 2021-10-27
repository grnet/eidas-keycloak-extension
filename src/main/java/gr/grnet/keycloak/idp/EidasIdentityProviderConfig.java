package gr.grnet.keycloak.idp;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class EidasIdentityProviderConfig extends SAMLIdentityProviderConfig {

	public static final String LEVEL_OF_ASSURANCE = "levelOfAssurance";

	public EidasIdentityProviderConfig() {
	}

	public EidasIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
		super(identityProviderModel);
	}

	public String getLevelOfAssurance() {
		return getConfig().get(LEVEL_OF_ASSURANCE);
	}

	public void setLevelOfAssurance(String levelOfAssurance) {
		getConfig().put(LEVEL_OF_ASSURANCE, levelOfAssurance);
	}

}
