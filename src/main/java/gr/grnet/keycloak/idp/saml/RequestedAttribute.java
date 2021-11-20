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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestedAttribute {

	@JsonAlias({ "Name", "name" })
	@JsonProperty("Name")
	private String name;

	@JsonAlias({ "NameFormat", "nameformat", "nameFormat", "Nameformat" })
	@JsonProperty("NameFormat")
	private String nameFormat;

	@JsonAlias({ "required", "isrequired", "IsRequired", "isRequired" })
	@JsonProperty("isRequired")
	private boolean required;

	public RequestedAttribute() {
	}

	public RequestedAttribute(String name, String nameFormat, boolean required) {
		this.name = name;
		this.nameFormat = nameFormat;
		this.required = required;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameFormat() {
		return nameFormat;
	}

	public void setNameFormat(String nameFormat) {
		this.nameFormat = nameFormat;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

}
