package gr.grnet.keycloak.idp.parsers;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.assertion.AbstractStaxSamlAssertionParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @since Oct 14, 2010
 */
public class EidasSAMLAttributeStatementParser extends AbstractStaxSamlAssertionParser<AttributeStatementType> {

    private static final EidasSAMLAttributeStatementParser INSTANCE = new EidasSAMLAttributeStatementParser();

    private EidasSAMLAttributeStatementParser() {
        super(SAMLAssertionQNames.ATTRIBUTE_STATEMENT);
    }

    public static EidasSAMLAttributeStatementParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected AttributeStatementType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new AttributeStatementType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AttributeStatementType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE:
                target.addAttribute(new ASTChoiceType(EidasSAMLAttributeParser.getInstance().parse(xmlEventReader)));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
