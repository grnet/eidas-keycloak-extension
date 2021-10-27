package gr.grnet.keycloak.idp;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLResponseWriter;
import org.w3c.dom.Document;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Writer;


public class EidasSAML2Request extends SAML2Request {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();


    /**
     * Return the DOM object
     *
     * @param rat
     *
     * @return
     *
     * @throws ProcessingException
     * @throws ParsingException
     * @throws ConfigurationException
     */
    public static Document convert(RequestAbstractType rat) throws ProcessingException, ConfigurationException, ParsingException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        EidasSAMLRequestWriter writer = new EidasSAMLRequestWriter(StaxUtil.getXMLStreamWriter(bos));
        if (rat instanceof AuthnRequestType) {
            writer.write((AuthnRequestType) rat);
        } else if (rat instanceof LogoutRequestType) {
            writer.write((LogoutRequestType) rat);
        }

        return DocumentUtil.getDocument(new String(bos.toByteArray(), GeneralConstants.SAML_CHARSET));
    }

    /**
     * Convert a SAML2 Response into a Document
     *
     * @param responseType
     *
     * @return
     *
     * @throws ProcessingException
     * @throws ParsingException
     * @throws ConfigurationException
     */
    public static Document convert(ResponseType responseType) throws ProcessingException, ParsingException, ConfigurationException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SAMLResponseWriter writer = new SAMLResponseWriter(StaxUtil.getXMLStreamWriter(baos));
        writer.write(responseType);

        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
        return DocumentUtil.getDocument(bis);
    }

    /**
     * Marshall the AuthnRequestType to an output stream
     *
     * @param requestType
     * @param os
     *
     * @throws ProcessingException
     */
    public static void marshall(RequestAbstractType requestType, OutputStream os) throws ProcessingException {
        EidasSAMLRequestWriter samlRequestWriter = new EidasSAMLRequestWriter(StaxUtil.getXMLStreamWriter(os));
        if (requestType instanceof AuthnRequestType) {
            samlRequestWriter.write((AuthnRequestType) requestType);
        } else if (requestType instanceof LogoutRequestType) {
            samlRequestWriter.write((LogoutRequestType) requestType);
        } else
            throw logger.unsupportedType(requestType.getClass().getName());
    }

    /**
     * Marshall the AuthnRequestType to a writer
     *
     * @param requestType
     * @param writer
     *
     * @throws ProcessingException
     */
    public static void marshall(RequestAbstractType requestType, Writer writer) throws ProcessingException {
        EidasSAMLRequestWriter samlRequestWriter = new EidasSAMLRequestWriter(StaxUtil.getXMLStreamWriter(writer));

        if (requestType instanceof AuthnRequestType) {
            samlRequestWriter.write((AuthnRequestType) requestType);
        } else if (requestType instanceof LogoutRequestType) {
            samlRequestWriter.write((LogoutRequestType) requestType);
        } else
            throw logger.unsupportedType(requestType.getClass().getName());
    }
}