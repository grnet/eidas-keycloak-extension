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
