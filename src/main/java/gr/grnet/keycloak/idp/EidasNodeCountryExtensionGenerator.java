package gr.grnet.keycloak.idp;

import javax.xml.stream.XMLStreamWriter;

import org.jboss.logging.Logger;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class EidasNodeCountryExtensionGenerator implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

	public static final String EIDAS_NS_URI = "http://eidas.europa.eu/saml-extensions";
	public static final String EIDAS_PREFIX = "eidas";

	protected static final Logger logger = Logger.getLogger(EidasNodeCountryExtensionGenerator.class);

	private String nodeCountry;
	
	public EidasNodeCountryExtensionGenerator(String nodeCountry) {
		this.nodeCountry = nodeCountry;
	}

	@Override
	public void write(XMLStreamWriter writer) throws ProcessingException {
		StaxUtil.writeNameSpace(writer, EIDAS_PREFIX, EIDAS_NS_URI);
		StaxUtil.writeStartElement(writer, EIDAS_PREFIX, "NodeCountry", EIDAS_NS_URI);
		StaxUtil.writeCharacters(writer, nodeCountry);
		StaxUtil.writeEndElement(writer);
		StaxUtil.flush(writer);
	}

}