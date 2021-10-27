package gr.grnet.keycloak.idp;

import org.keycloak.Config.Scope;
// import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.validators.DestinationValidator;

public class EidasIdentityProviderFactory extends SAMLIdentityProviderFactory {

    public static final String PROVIDER_ID = "eidasSaml";

    private DestinationValidator destinationValidator;

	@Override
	public String getId() {
		
		return PROVIDER_ID;
	}
	
	@Override
	public String getName() {
		
		return "eID.AS SAML";
	}
	
	@Override
    public EidasIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new EidasIdentityProvider(session, new EidasIdentityProviderConfig(model), destinationValidator);
    }
	
    @Override
    public void init(Scope config) {
        super.init(config);

        this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
    }
	
}
