package gr.grnet.keycloak.idp.parsers;

import java.security.PrivateKey;

import javax.xml.namespace.QName;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.util.JAXPValidationUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class EidasAssertionUtil  {

	/**
     * This method modifies the given responseType, and replaces the encrypted assertion with a decrypted version.
     * @param responseType a response containg an encrypted assertion
     * @return the assertion element as it was decrypted. This can be used in signature verification.
     */
    public static Element decryptAssertion(SAMLDocumentHolder holder, ResponseType responseType, PrivateKey privateKey) throws ParsingException, ProcessingException, ConfigurationException {
        Document doc = holder.getSamlDocument();
        Element enc = DocumentUtil.getElement(doc, new QName(JBossSAMLConstants.ENCRYPTED_ASSERTION.get()));

        if (enc == null) {
            throw new ProcessingException("No encrypted assertion found.");
        }

        String oldID = enc.getAttribute(JBossSAMLConstants.ID.get());
        Document newDoc = DocumentUtil.createDocument();
        Node importedNode = newDoc.importNode(enc, true);
        newDoc.appendChild(importedNode);

        Element decryptedDocumentElement = XMLEncryptionUtil.decryptElementInDocument(newDoc, privateKey);
        EidasSAMLParser parser = EidasSAMLParser.getInstance();

        JAXPValidationUtil.checkSchemaValidation(decryptedDocumentElement);
        AssertionType assertion = (AssertionType) parser.parse(parser.createEventReader(DocumentUtil
                .getNodeAsStream(decryptedDocumentElement)));

        responseType.replaceAssertion(oldID, new ResponseType.RTChoiceType(assertion));

        return decryptedDocumentElement;
    }
	
}
