/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates and other 
 * contributors as indicated by the @author tags.
 * 
 * eIDAS modifications, Copyright 2021 GRNET, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gr.grnet.keycloak.idp.saml;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.ExtensionsType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;
import org.w3c.dom.Element;

public class EidasSAMLMetadataWriter extends SAMLMetadataWriter {

	private final String EIDAS_NS_URI = "http://eidas.europa.eu/saml-extensions";
	private final String EIDAS_PREFIX = "eidas";
	private final String METADATA_PREFIX = "md";
	
	public EidasSAMLMetadataWriter(XMLStreamWriter writer) {
		super(writer);
	}
	
	@Override
    public void write(SPSSODescriptorType spSSODescriptor) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.SP_SSO_DESCRIPTOR.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        // eIDAS namespace
        StaxUtil.writeNameSpace(writer, EIDAS_PREFIX, EIDAS_NS_URI);
        
        writeProtocolSupportEnumeration(spSSODescriptor.getProtocolSupportEnumeration());

        // Write the attributes
        Boolean authnSigned = spSSODescriptor.isAuthnRequestsSigned();
        if (authnSigned != null) {
            StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.AUTHN_REQUESTS_SIGNED.get()),
                    authnSigned.toString());
        }
        Boolean wantAssertionsSigned = spSSODescriptor.isWantAssertionsSigned();
        if (wantAssertionsSigned != null) {
            StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.WANT_ASSERTIONS_SIGNED.get()),
                    wantAssertionsSigned.toString());
        }
        
        // eIDAS specific to add extensions 
        ExtensionsType extensions = spSSODescriptor.getExtensions();
        if (extensions != null) {
            write(extensions);
        }

        // Get the key descriptors
        List<KeyDescriptorType> keyDescriptors = spSSODescriptor.getKeyDescriptor();
        for (KeyDescriptorType keyDescriptor : keyDescriptors) {
            writeKeyDescriptor(keyDescriptor);
        }

        List<EndpointType> sloServices = spSSODescriptor.getSingleLogoutService();
        for (EndpointType endpoint : sloServices) {
            writeSingleLogoutService(endpoint);
        }

        List<IndexedEndpointType> artifactResolutions = spSSODescriptor.getArtifactResolutionService();
        for (IndexedEndpointType artifactResolution : artifactResolutions) {
            writeArtifactResolutionService(artifactResolution);
        }

        List<String> nameIDFormats = spSSODescriptor.getNameIDFormat();
        for (String nameIDFormat : nameIDFormats) {
            writeNameIDFormat(nameIDFormat);
        }

        List<IndexedEndpointType> assertionConsumers = spSSODescriptor.getAssertionConsumerService();
        for (IndexedEndpointType assertionConsumer : assertionConsumers) {
            writeAssertionConsumerService(assertionConsumer);
        }

        List<AttributeConsumingServiceType> attributeConsumers = spSSODescriptor.getAttributeConsumingService();
        for (AttributeConsumingServiceType attributeConsumer : attributeConsumers) {
            writeAttributeConsumingService(attributeConsumer);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }
	
	@Override
    public void write(ExtensionsType extensions) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.EXTENSIONS__METADATA.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        for (Object o: extensions.getAny()) { 
        	if (o instanceof Element) {
        		StaxUtil.writeDOMElement(writer, (Element) o);
            } else if (o instanceof SamlProtocolExtensionsAwareBuilder.NodeGenerator) {
                SamlProtocolExtensionsAwareBuilder.NodeGenerator ng = (SamlProtocolExtensionsAwareBuilder.NodeGenerator) o;
                ng.write(writer);
            }
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }
	
    private void writeProtocolSupportEnumeration(List<String> protoEnum) throws ProcessingException {
        if (protoEnum.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String str : protoEnum) {
                sb.append(str).append(" ");
            }

            StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.PROTOCOL_SUPPORT_ENUMERATION.get()), sb.toString()
                    .trim());
        }
    }

    private void writeNameIDFormat(String nameIDFormat) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.NAMEID_FORMAT.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        if (nameIDFormat != null) {
            StaxUtil.writeCharacters(writer, nameIDFormat);
        }

        StaxUtil.writeEndElement(writer);
    }
	
}
