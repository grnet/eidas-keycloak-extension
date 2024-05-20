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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.keycloak.Config.Scope;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.saml.validators.DestinationValidator;

public class EidasSAMLIdentityProviderFactory implements IdentityProviderFactory<EidasSAMLIdentityProvider> {

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
	public EidasSAMLIdentityProvider create(KeycloakSession session) {
		return null;
	}

	@Override
	public void init(Scope config) {
		this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
			return ProviderConfigurationBuilder.create()
					.property()
					.name(EidasSAMLIdentityProviderConfig.SERVICE_PROVIDER_COUNTRY_OF_ORIGIN)
					.type(ProviderConfigProperty.STRING_TYPE)
					.label("Service Provider Country of Origin")
					.helpText("identity-provider.eidas-saml.service-provider-country-of-origin.tooltip")
				.add()			
					.property()
					.name(EidasSAMLIdentityProviderConfig.LEVEL_OF_ASSURANCE)
					.type(ProviderConfigProperty.STRING_TYPE)
					.label("Level of Assurance")
					.helpText("identity-provider.eidas-saml.level-of-assurance.tooltip")
				.add()
					.property()
					.name(EidasSAMLIdentityProviderConfig.PRIVATE_SERVICE_PROVIDER)
					.type(ProviderConfigProperty.BOOLEAN_TYPE)
					.label("Private Service Provider")
					.helpText("identity-provider.eidas-saml.private-service-provider.tooltip")
    			.add()								
					.property()
					.name(EidasSAMLIdentityProviderConfig.REQUESTED_ATTRIBUTES)
					.type(ProviderConfigProperty.STRING_TYPE)
					.label("Requested Attributes")
					.helpText("identity-provider.eidas-saml.requested-attributes.tooltip")
				.add()
			.build();
		}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	@Override
	public Map<String, String> parseConfig(KeycloakSession session, InputStream inputStream) {
		throw new UnsupportedOperationException("Unimplemented method 'parseConfig'");
	}

}
