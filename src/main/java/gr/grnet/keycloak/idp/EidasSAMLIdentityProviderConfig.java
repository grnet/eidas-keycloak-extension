package gr.grnet.keycloak.idp;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class EidasSAMLIdentityProviderConfig extends SAMLIdentityProviderConfig {

	private static final long serialVersionUID = 3296255033084690635L;

	public static final String LEVEL_OF_ASSURANCE = "levelOfAssurance";
	public static final String PRIVATE_SERVICE_PROVIDER = "privateServiceProvider";
	public static final String REQUESTED_ATTRIBUTES = "requestedAttributes";
	public static final String SERVICE_PROVIDER_COUNTRY_OF_ORIGIN = "serviceProviderCountryOfOrigin";

	public EidasSAMLIdentityProviderConfig() {
	}

	public EidasSAMLIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
		super(identityProviderModel);
	}

	public String getLevelOfAssurance() {
		return getConfig().get(LEVEL_OF_ASSURANCE);
	}

	public void setLevelOfAssurance(String levelOfAssurance) {
		getConfig().put(LEVEL_OF_ASSURANCE, levelOfAssurance);
	}

	public boolean isPrivateServiceProvider() {
		return Boolean.valueOf(getConfig().get(PRIVATE_SERVICE_PROVIDER));
	}

	public void setPrivateServiceProvider(boolean privateServiceProvider) {
		getConfig().put(PRIVATE_SERVICE_PROVIDER, String.valueOf(privateServiceProvider));
	}

	public void setRequestedAttributes(String requestedAttributes) {
		getConfig().put(REQUESTED_ATTRIBUTES, requestedAttributes);
	}

	public String getRequestedAttributes() {
		return getConfig().get(REQUESTED_ATTRIBUTES);
	}

	public String getServiceProviderCountryOfOrigin() {
		return getConfig().get(SERVICE_PROVIDER_COUNTRY_OF_ORIGIN);
	}

	public void setServiceProviderCountryOfOrigin(String serviceProviderCountryOfOrigin) {
		getConfig().put(SERVICE_PROVIDER_COUNTRY_OF_ORIGIN, serviceProviderCountryOfOrigin);
	}

}
