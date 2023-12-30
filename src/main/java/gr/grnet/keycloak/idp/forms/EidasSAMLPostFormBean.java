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
package gr.grnet.keycloak.idp.forms;

import org.keycloak.saml.common.constants.GeneralConstants;

import gr.grnet.keycloak.idp.saml.EidasJaxrsSAML2BindingBuilder;
import jakarta.ws.rs.core.MultivaluedMap;

public class EidasSAMLPostFormBean {

	private final String samlRequest;
	private final String samlResponse;
	private final String relayState;
	private final String country;
	private final String url;

	public EidasSAMLPostFormBean(MultivaluedMap<String, String> formData) {
		samlRequest = formData.getFirst(GeneralConstants.SAML_REQUEST_KEY);
		samlResponse = formData.getFirst(GeneralConstants.SAML_RESPONSE_KEY);
		relayState = formData.getFirst(GeneralConstants.RELAY_STATE);
		country = formData.getFirst(EidasJaxrsSAML2BindingBuilder.COUNTRY);
		url = formData.getFirst(GeneralConstants.URL);
	}

	public String getSAMLRequest() {
		return samlRequest;
	}

	public String getSAMLResponse() {
		return samlResponse;
	}

	public String getRelayState() {
		return relayState;
	}

	public String getCountry() {
		return country;
	}

	public String getUrl() {
		return url;
	}
}
