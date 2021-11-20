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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * An authenticator which presents the user with a country selection form. The
 * results is stored in an authentication note, in order to be used later in the
 * flow.
 */
public class CitizenCountrySelectorAuthenticatorForm implements Authenticator {

	private static final Logger LOG = Logger.getLogger(CitizenCountrySelectorAuthenticatorForm.class);

	/**
	 * Property added in user's session note.
	 */
	public static final String CITIZEN_COUNTRY = "citizen.country";

	public CitizenCountrySelectorAuthenticatorForm() {
	}

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		// get countries list from configuration
		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		String countriesList = config.getConfig()
				.get(CitizenCountrySelectorAuthenticatorFormFactory.CITIZEN_COUNTRY_LIST);
		List<String> countries;

		if (countriesList == null) {
			countries = new ArrayList<>();
		} else {
			countries = Arrays.asList(countriesList.split("##"));
		}

		LOG.debug("Countries from config = " + countries);

		Response response = context.form().setAttribute("availablecountries", countries)
				.createForm("citizen-country-select-form.ftl");

		context.challenge(response);
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String country = formData.getFirst("country");

		LOG.debugf("Retrieved country=%s", country);

		if (country != null && !country.trim().isEmpty()) {
			// Add selected information to auth note
			context.getAuthenticationSession().setAuthNote(CITIZEN_COUNTRY, country);
		}

		context.success();
	}

	@Override
	public void close() {
		// NOOP
	}
}
