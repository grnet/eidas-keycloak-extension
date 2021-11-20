/*
 * Copyright 2021 GRNET, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLStreamWriter;

import org.jboss.logging.Logger;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.util.JsonSerialization;

import gr.grnet.keycloak.idp.EidasSAMLIdentityProviderConfig;

public class EidasAuthnExtensionGenerator implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

	public static final String EIDAS_NS_URI = "http://eidas.europa.eu/saml-extensions";
	public static final String EIDAS_PREFIX = "eidas";

	protected static final Logger logger = Logger.getLogger(EidasAuthnExtensionGenerator.class);

	private EidasSAMLIdentityProviderConfig config;

	public EidasAuthnExtensionGenerator(EidasSAMLIdentityProviderConfig config) {
		this.config = config;
	}

	@Override
	public void write(XMLStreamWriter writer) throws ProcessingException {
		StaxUtil.writeNameSpace(writer, EIDAS_PREFIX, EIDAS_NS_URI);

		StaxUtil.writeStartElement(writer, EIDAS_PREFIX, "SPType", EIDAS_NS_URI);
		if (config.isPrivateServiceProvider()) {
			StaxUtil.writeCharacters(writer, "private");
		} else {
			StaxUtil.writeCharacters(writer, "public");
		}
		StaxUtil.writeEndElement(writer);

		List<RequestedAttribute> requestedAttributes = getRequestedAttributes();
		if (!requestedAttributes.isEmpty()) {
			StaxUtil.writeStartElement(writer, EIDAS_PREFIX, "RequestedAttributes", EIDAS_NS_URI);

			for (RequestedAttribute ra : requestedAttributes) {
				StaxUtil.writeStartElement(writer, EIDAS_PREFIX, "RequestedAttribute", EIDAS_NS_URI);
				StaxUtil.writeAttribute(writer, "Name", ra.getName());
				StaxUtil.writeAttribute(writer, "NameFormat", ra.getNameFormat());
				StaxUtil.writeAttribute(writer, "isRequired", String.valueOf(ra.isRequired()));
				StaxUtil.writeEndElement(writer);
			}

			StaxUtil.writeEndElement(writer);
		}

		StaxUtil.flush(writer);
	}

	private List<RequestedAttribute> getRequestedAttributes() {
		String requestedAttributes = config.getRequestedAttributes();
		if (requestedAttributes == null || requestedAttributes.isEmpty())
			return new ArrayList<>();
		try {
			return Arrays.asList(JsonSerialization.readValue(requestedAttributes, RequestedAttribute[].class));
		} catch (Exception e) {
			logger.warn("Could not json-deserialize RequestedAttribute config entry: " + requestedAttributes, e);
			return new ArrayList<>();
		}
	}
}