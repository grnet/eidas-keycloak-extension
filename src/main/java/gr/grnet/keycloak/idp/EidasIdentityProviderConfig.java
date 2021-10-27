package gr.grnet.keycloak.idp;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class EidasIdentityProviderConfig extends SAMLIdentityProviderConfig {
	
    public EidasIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }
    public static final String ATTRIBUTE_CONSUMING_SERVICE_INDEX = "attributeConsumingServiceIndex";
    public static final String EIDAS_LOA = "http://eidas.europa.eu/LoA/high";
    public String getLOA(){

        return getConfig().get(EIDAS_LOA);
    }

    public void setLOA(String new_loa){
        getConfig().put(EIDAS_LOA,new_loa);
    }

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
