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

import javax.xml.stream.XMLStreamWriter;

import org.jboss.logging.Logger;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class EidasNodeCountryExtensionGenerator implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

	public static final String EIDAS_NS_URI = "http://eidas.europa.eu/saml-extensions";
	public static final String EIDAS_PREFIX = "eidas";
	public static final String pattern = "^[A-Za-z][A-Za-z]$";

	protected static final Logger logger = Logger.getLogger(EidasNodeCountryExtensionGenerator.class);

	private String nodeCountry;
	
	public EidasNodeCountryExtensionGenerator(String nodeCountry) {
		this.nodeCountry = nodeCountry;
	}

	@Override
	public void write(XMLStreamWriter writer) throws ProcessingException {
		if (!nodeCountry.trim().matches(pattern)) { 
			logger.debug("Skipping invalid node country code.");
			return;
		}
		
		StaxUtil.writeNameSpace(writer, EIDAS_PREFIX, EIDAS_NS_URI);
		StaxUtil.writeStartElement(writer, EIDAS_PREFIX, "NodeCountry", EIDAS_NS_URI);
		StaxUtil.writeCharacters(writer, nodeCountry);
		StaxUtil.writeEndElement(writer);
		StaxUtil.flush(writer);
	}

}