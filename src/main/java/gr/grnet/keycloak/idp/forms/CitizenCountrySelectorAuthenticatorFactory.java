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
package gr.grnet.keycloak.idp.forms;

import static org.keycloak.provider.ProviderConfigProperty.MULTIVALUED_STRING_TYPE;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class CitizenCountrySelectorAuthenticatorFactory implements AuthenticatorFactory {

	public static final String PROVIDER_ID = "auth-select-citizen-country";
	public static final CitizenCountrySelectorAuthenticator SINGLETON = new CitizenCountrySelectorAuthenticator();

	public static final String CITIZEN_COUNTRY_LIST = "citizenCountryList";

	protected static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
			AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.ALTERNATIVE,
			AuthenticationExecutionModel.Requirement.DISABLED };

	@Override
	public String getDisplayType() {
		return "Citizen Country Selection";
	}

	@Override
	public String getReferenceCategory() {
		return null;
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}

	@Override
	public String getHelpText() {
		return "Citizen Country Selection.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		ProviderConfigProperty rep = new ProviderConfigProperty(CITIZEN_COUNTRY_LIST, "Citizen Country List",
				"List of supported citizen's countries.", MULTIVALUED_STRING_TYPE, null);
		return Collections.singletonList(rep);
	}

	@Override
	public void close() {
		// NOOP
	}

	@Override
	public Authenticator create(KeycloakSession session) {
		return SINGLETON;
	}

	@Override
	public void init(Config.Scope config) {
		// NOOP
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		// NOOP
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
