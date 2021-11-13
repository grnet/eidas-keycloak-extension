package gr.grnet.keycloak.idp.parsers;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.ResponseType.RTChoiceType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLEncryptedAssertionParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLExtensionsParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLProtocolQNames;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLStatusParser;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLStatusResponseTypeParser;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Element;

/**
 * Parse the SAML Response
 *
 * @since Nov 2, 2010
 */
public class EidasSAMLResponseParser extends SAMLStatusResponseTypeParser<ResponseType> {

    private static final EidasSAMLResponseParser INSTANCE = new EidasSAMLResponseParser();

    private EidasSAMLResponseParser() {
        super(SAMLProtocolQNames.RESPONSE);
    }

    public static EidasSAMLResponseParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected ResponseType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        SAMLParserUtil.validateAttributeValue(element, SAMLProtocolQNames.ATTR_VERSION, VERSION_2_0);
        String id = StaxParserUtil.getRequiredAttributeValue(element, SAMLProtocolQNames.ATTR_ID);
        XMLGregorianCalendar issueInstant = XMLTimeUtil.parse(StaxParserUtil.getRequiredAttributeValue(element, SAMLProtocolQNames.ATTR_ISSUE_INSTANT));

        ResponseType res = new ResponseType(id, issueInstant);

        // Let us set the attributes
        super.parseBaseAttributes(element, res);

        return res;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, ResponseType target, SAMLProtocolQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ISSUER:
                target.setIssuer(SAMLParserUtil.parseNameIDType(xmlEventReader));
                break;

            case SIGNATURE:
                Element sig = StaxParserUtil.getDOMElement(xmlEventReader);
                target.setSignature(sig);
                break;

            case ASSERTION:
                target.addAssertion(new RTChoiceType(EidasSAMLAssertionParser.getInstance().parse(xmlEventReader)));
                break;

            case EXTENSIONS:
                target.setExtensions(SAMLExtensionsParser.getInstance().parse(xmlEventReader));
                break;

            case STATUS:
                target.setStatus(SAMLStatusParser.getInstance().parse(xmlEventReader));
                break;

            case ENCRYPTED_ASSERTION:
                target.addAssertion(new RTChoiceType(SAMLEncryptedAssertionParser.getInstance().parse(xmlEventReader)));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
