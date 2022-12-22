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
package gr.grnet.keycloak.idp.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.util.StringUtil;

import gr.grnet.keycloak.idp.EidasSAMLIdentityProviderFactory;

public class EidasPersonIdentifierAndCountryExtractor extends AbstractIdentityProviderMapper {

	public static final String[] COMPATIBLE_PROVIDERS = { EidasSAMLIdentityProviderFactory.PROVIDER_ID };

	private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

	protected static final Logger logger = Logger.getLogger(EidasUserAttributeMapper.class);
	
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName("country_user_attribute_dest");
        property.setLabel("Country user attribute dest");
        property.setHelpText("Where should we store the extracted country");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName("personID_user_attribute_dest");
        property.setLabel("Person ID user attribute dest");
        property.setHelpText("Where should we store the extracted person ID");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "eidas-saml-person-identifier-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "PersonIdentifier Extractor";
    }

    @Override
    public String getDisplayType() {
        return "PersonIdentifier Extractor";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
      
        List<String> attributeValuesInContext = findAttributeValuesInContext("PersonIdentifier", context);
        if (!attributeValuesInContext.isEmpty()) {
            String[] data = attributeValuesInContext.get(0).split("/");
            if(data.length == 3){
                String country_dest_field = mapperModel.getConfig().get("country_user_attribute_dest");
                if(StringUtil.isNullOrEmpty(country_dest_field)){
                   context.setUserAttribute("country", data[0]);
                }else{
                   context.setUserAttribute(country_dest_field, data[0]);
                }
                String personId_dest_field = mapperModel.getConfig().get("personID_user_attribute_dest");
                if(StringUtil.isNullOrEmpty(personId_dest_field)){
                   context.setUserAttribute("PersonIdentifier", data[2]);
                }else{
                   context.setUserAttribute(personId_dest_field, data[2]);
                }
                //data[1] is our SP country
            }
            
        }
    }

    private Predicate<AttributeStatementType.ASTChoiceType> elementWith(String attributeName) {
        return attributeType -> {
            AttributeType attribute = attributeType.getAttribute();
            return Objects.equals(attribute.getName(), attributeName)
                    || Objects.equals(attribute.getFriendlyName(), attributeName);
        };
    }


    private List<String> findAttributeValuesInContext(String attributeName, BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

        return assertion.getAttributeStatements().stream()
                .flatMap(statement -> statement.getAttributes().stream())
                .filter(elementWith(attributeName))
                .flatMap(attributeType -> attributeType.getAttribute().getAttributeValue().stream())
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        
        List<String> attributeValuesInContext = findAttributeValuesInContext("PersonIdentifier", context);
        String[] data = attributeValuesInContext.get(0).split("/");
        if(data.length == 3){
            String country_dest_field = mapperModel.getConfig().get("country_user_attribute_dest");
            if(StringUtil.isNullOrEmpty(country_dest_field)){
                user.setAttribute("country", Arrays.asList(data[0]));
            }else{
                user.setAttribute(country_dest_field, Arrays.asList(data[0]));
            }
            String personId_dest_field = mapperModel.getConfig().get("personID_user_attribute_dest");
            if(StringUtil.isNullOrEmpty(personId_dest_field)){
                user.setAttribute("PersonIdentifier", Arrays.asList(data[2]));
            }else{
                user.setAttribute(personId_dest_field, Arrays.asList(data[2]));
            }
            //data[1] is our SP country
        }
    }

    @Override
    public String getHelpText() {
        return "Extract the eidas attribute PersonIdentifier to get country and person ID.";
    }	
}
