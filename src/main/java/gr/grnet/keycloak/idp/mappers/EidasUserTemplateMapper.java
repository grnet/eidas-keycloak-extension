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
package gr.grnet.keycloak.idp.mappers;

import org.keycloak.broker.saml.mappers.UsernameTemplateMapper;

import gr.grnet.keycloak.idp.EidasSAMLIdentityProviderFactory;

public class EidasUserTemplateMapper extends UsernameTemplateMapper {

    public static final String[] COMPATIBLE_PROVIDERS = { EidasSAMLIdentityProviderFactory.PROVIDER_ID };

    public static final String PROVIDER_ID = "eidas-saml-username-idp-mapper";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }
	
}
