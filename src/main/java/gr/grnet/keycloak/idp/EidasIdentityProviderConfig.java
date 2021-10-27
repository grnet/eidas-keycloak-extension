package gr.grnet.keycloak.idp;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class EidasIdentityProviderConfig extends SAMLIdentityProviderConfig {
	public String eidasLOA;

    public EidasIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);

        this.eidasLOA = getConfig().get("eidasLOA");
        
    }
    public static final String ATTRIBUTE_CONSUMING_SERVICE_INDEX = "attributeConsumingServiceIndex";

    public Integer getAttributeConsumingServiceIndex() {
        Integer result = null;

        String strAttributeConsumingServiceIndex = getConfig().get(ATTRIBUTE_CONSUMING_SERVICE_INDEX);
        if (strAttributeConsumingServiceIndex != null && !strAttributeConsumingServiceIndex.isEmpty()) {
            try {
                result = Integer.parseInt(strAttributeConsumingServiceIndex);
                if (result < 0) {
                    result = null;
                }
            } catch (NumberFormatException e) {
                // ignore it and use null
            }
        }
        return result;
    }
}
