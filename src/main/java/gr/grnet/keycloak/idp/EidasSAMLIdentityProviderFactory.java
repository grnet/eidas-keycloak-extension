package gr.grnet.keycloak.idp;

import org.keycloak.Config.Scope;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.validators.DestinationValidator;

public class EidasSAMLIdentityProviderFactory extends SAMLIdentityProviderFactory {

	public static final String PROVIDER_ID = "eidas-saml";

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
	public void init(Scope config) {
		super.init(config);

		this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
	}

}
