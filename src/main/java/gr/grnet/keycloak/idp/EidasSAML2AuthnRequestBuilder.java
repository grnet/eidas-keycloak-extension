package gr.grnet.keycloak.idp;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.w3c.dom.Document;

public class EidasSAML2AuthnRequestBuilder extends SAML2AuthnRequestBuilder {

	public Document toDocument() {
		try {
			AuthnRequestType authnRequestType = createAuthnRequest();

			return EidasSAML2Request.convert(authnRequestType);
		} catch (Exception e) {
			throw new RuntimeException("Could not convert  to a document.", e);
		}
	}

}