package gr.grnet.keycloak.idp;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;

import java.net.URI;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLRequestWriter;
import org.w3c.dom.Element;

/**
 * Writes a eIDAS SAML2 Request Type to Stream
 */
public class EidasSAMLRequestWriter extends SAMLRequestWriter {

	public static final String EIDAS_PREFIX = "eidas";
	public static final String EIDAS_NS = "http://eidas.europa.eu/saml-extensions";

	public EidasSAMLRequestWriter(XMLStreamWriter writer) {
		super(writer);
	}

	/**
	 * Write a {@code AuthnRequestType } to stream
	 *
	 * @param request
	 *
	 * @throws org.keycloak.saml.common.exceptions.ProcessingException
	 */
	public void write(AuthnRequestType request) throws ProcessingException {
		StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.AUTHN_REQUEST.get(),
				PROTOCOL_NSURI.get());
		StaxUtil.writeNameSpace(writer, PROTOCOL_PREFIX, PROTOCOL_NSURI.get());
		StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());
		StaxUtil.writeNameSpace(writer, EIDAS_PREFIX, EIDAS_NS);
		StaxUtil.writeDefaultNameSpace(writer, ASSERTION_NSURI.get());

		// Attributes
		StaxUtil.writeAttribute(writer, JBossSAMLConstants.ID.get(), request.getID());
		StaxUtil.writeAttribute(writer, JBossSAMLConstants.VERSION.get(), request.getVersion());
		StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISSUE_INSTANT.get(), request.getIssueInstant().toString());

		URI destination = request.getDestination();
		if (destination != null)
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.DESTINATION.get(), destination.toASCIIString());

		String consent = request.getConsent();
		if (StringUtil.isNotNull(consent))
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.CONSENT.get(), consent);

		URI assertionURL = request.getAssertionConsumerServiceURL();
		if (assertionURL != null)
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE_URL.get(),
					assertionURL.toASCIIString());

		Boolean forceAuthn = request.isForceAuthn();
		if (forceAuthn != null) {
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.FORCE_AUTHN.get(), forceAuthn.toString());
		}

		Boolean isPassive = request.isIsPassive();
		// The AuthnRequest IsPassive attribute is optional and if omitted its default
		// value is false.
		// Some IdPs refuse requests if the IsPassive attribute is present and set to
		// false, so to
		// maximize compatibility we emit it only if it is set to true
		if (isPassive != null && isPassive == true) {
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.IS_PASSIVE.get(), isPassive.toString());
		}

		URI protocolBinding = request.getProtocolBinding();
		if (protocolBinding != null) {
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.PROTOCOL_BINDING.get(), protocolBinding.toString());
		}

		Integer assertionIndex = request.getAssertionConsumerServiceIndex();
		if (assertionIndex != null) {
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE_INDEX.get(),
					assertionIndex.toString());
		}

		Integer attrIndex = request.getAttributeConsumingServiceIndex();
		if (attrIndex != null) {
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.ATTRIBUTE_CONSUMING_SERVICE_INDEX.get(),
					attrIndex.toString());
		}
		String providerName = request.getProviderName();
		if (StringUtil.isNotNull(providerName)) {
			StaxUtil.writeAttribute(writer, JBossSAMLConstants.PROVIDER_NAME.get(), providerName);
		}

		NameIDType issuer = request.getIssuer();
		if (issuer != null) {
			write(issuer, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.ISSUER.get(), ASSERTION_PREFIX), false);
		}

		SubjectType subject = request.getSubject();
		if (subject != null) {
			write(subject);
		}

		Element sig = request.getSignature();
		if (sig != null) {
			StaxUtil.writeDOMElement(writer, sig);
		}

		ExtensionsType extensions = request.getExtensions();
		if (extensions != null && !extensions.getAny().isEmpty()) {
			write(extensions);
		}

		NameIDPolicyType nameIDPolicy = request.getNameIDPolicy();
		if (nameIDPolicy != null) {
			write(nameIDPolicy);
		}

		RequestedAuthnContextType requestedAuthnContext = request.getRequestedAuthnContext();
		if (requestedAuthnContext != null) {
			write(requestedAuthnContext);
		}

		StaxUtil.writeEndElement(writer);
		StaxUtil.flush(writer);
	}

}