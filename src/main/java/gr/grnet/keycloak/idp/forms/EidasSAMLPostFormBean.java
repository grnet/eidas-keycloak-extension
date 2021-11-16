package gr.grnet.keycloak.idp.forms;

import javax.ws.rs.core.MultivaluedMap;
import org.keycloak.saml.common.constants.GeneralConstants;

import gr.grnet.keycloak.idp.saml.EidasJaxrsSAML2BindingBuilder;

public class EidasSAMLPostFormBean {

	private final String samlRequest;
	private final String samlResponse;
	private final String relayState;
	private final String country;
	private final String url;

	public EidasSAMLPostFormBean(MultivaluedMap<String, String> formData) {
		samlRequest = formData.getFirst(GeneralConstants.SAML_REQUEST_KEY);
		samlResponse = formData.getFirst(GeneralConstants.SAML_RESPONSE_KEY);
		relayState = formData.getFirst(GeneralConstants.RELAY_STATE);
		country = formData.getFirst(EidasJaxrsSAML2BindingBuilder.COUNTRY);
		url = formData.getFirst(GeneralConstants.URL);
	}

	public String getSAMLRequest() {
		return samlRequest;
	}

	public String getSAMLResponse() {
		return samlResponse;
	}

	public String getRelayState() {
		return relayState;
	}

	public String getCountry() {
		return country;
	}

	public String getUrl() {
		return url;
	}
}
