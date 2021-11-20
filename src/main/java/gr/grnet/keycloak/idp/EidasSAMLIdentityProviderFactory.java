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
package gr.grnet.keycloak.idp;

import org.keycloak.Config.Scope;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.validators.DestinationValidator;

public class EidasSAMLIdentityProviderFactory extends SAMLIdentityProviderFactory {

	public static final String PROVIDER_ID = "eidas-saml";

	private DestinationValidator destinationValidator;

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getName() {
		return "eIDAS SAML v2.0";
	}

	@Override
	public EidasSAMLIdentityProviderConfig createConfig() {
		return new EidasSAMLIdentityProviderConfig();
	}

	@Override
	public EidasSAMLIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
		return new EidasSAMLIdentityProvider(session, new EidasSAMLIdentityProviderConfig(model), destinationValidator);
	}

	@Override
	public void init(Scope config) {
		super.init(config);

		this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
	}

}
