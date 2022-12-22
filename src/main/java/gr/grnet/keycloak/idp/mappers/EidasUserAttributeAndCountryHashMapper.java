/*
 * Copyright 2022 GRNET.
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

import gr.grnet.keycloak.idp.EidasSAMLIdentityProviderFactory;

import static org.keycloak.broker.saml.mappers.UsernameTemplateMapper.TARGET;
import static org.keycloak.broker.saml.mappers.UsernameTemplateMapper.TARGETS;
import static org.keycloak.broker.saml.mappers.UsernameTemplateMapper.getTarget;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.mappers.UsernameTemplateMapper.Target;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.broker.saml.SAMLEndpoint;


public class EidasUserAttributeAndCountryHashMapper extends AbstractIdentityProviderMapper {
    protected static final Logger logger = Logger.getLogger(EidasUserAttributeAndCountryHashMapper.class);
    
    public static final String[] COMPATIBLE_PROVIDERS = { EidasSAMLIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(
            Arrays.asList(IdentityProviderSyncMode.values()));

    public static final String SALT = "salt";
    public static final String SALT_DEFAULT_VALUE = "a salt value";
    public static final String LENGTH = "length";
    public static final int LENGTH_DEFAULT_VALUE = 12;
    public static final int LENGTH_MAX_VALUE = 12;
    public static final String PREFIX = "prefix";
    public static final String PREFIX_DEFAULT_VALUE = "user";

    public static final int[] DIGITS = { 0, 3, 5, 7, 12, 17, 19, 21, 25, 32, 40, 51 };

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(SALT);
        property.setLabel("Salt");
        property.setHelpText("Salt to use.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(SALT_DEFAULT_VALUE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LENGTH);
        property.setLabel("Length");
        property.setHelpText("Length of user id.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(String.valueOf(LENGTH_DEFAULT_VALUE));
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(PREFIX);
        property.setLabel("Prefix");
        property.setHelpText("Prefix for username.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(PREFIX_DEFAULT_VALUE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TARGET);
        property.setLabel("Target");
        property.setHelpText(
                "Destination field for the mapper. LOCAL (default) means that the changes are applied to the username stored in local database upon user import. BROKER_ID and BROKER_USERNAME means that the changes are stored into the ID or username used for federation user lookup, respectively.");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(TARGETS);
        property.setDefaultValue(Target.LOCAL.toString());
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "eidas-username-attribute-and-country-hash-mapper";

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
        return "Preprocessor";
    }

    @Override
    public String getDisplayType() {
        return "Username Attribute and Country Hash Mapper";
    }

    @Override
    public void updateBrokeredUserLegacy(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // preprocessFederatedIdentity gets called anyways, so we only need to set the
        // username if necessary.
        // However, we don't want to set the username when the email is used as username
        if (getTarget(mapperModel.getConfig().get(TARGET)) == Target.LOCAL && !realm.isRegistrationEmailAsUsername()) {
            user.setUsername(context.getModelUsername());
        }
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        setUserNameByHashingClaimValue(mapperModel, context);
    }

    private void setUserNameByHashingClaimValue(IdentityProviderMapperModel mapperModel,
            BrokeredIdentityContext context) {
        // read configuration
        String salt = mapperModel.getConfig().getOrDefault(SALT, SALT_DEFAULT_VALUE);

        String prefix = mapperModel.getConfig().getOrDefault(PREFIX, PREFIX_DEFAULT_VALUE);
        String lengthAsString = mapperModel.getConfig().getOrDefault(LENGTH, String.valueOf(LENGTH_DEFAULT_VALUE));
        int length = LENGTH_DEFAULT_VALUE;
        try {
            length = Integer.parseInt(lengthAsString);
            if (length > LENGTH_MAX_VALUE) {
                length = LENGTH_DEFAULT_VALUE;
            }
        } catch (NumberFormatException e) {
            // ignore
        }

        // find claim
        List<String> attributeValueList = findAttributeValuesInContext("PersonIdentifier",context);
        if (!attributeValueList.isEmpty()) {
            String[] data = attributeValueList.get(0).split("/");
            if(data.length == 3){
                // hash
                String hash = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, salt).hmacHex(data[2] + ":" + data[0]);

                // build username
                StringBuffer sb = new StringBuffer();
                sb.append(prefix);
                for (int i = 0; i < length; i++) {
                    sb.append(hash.charAt(DIGITS[i]));
                }

                Target t = getTarget(mapperModel.getConfig().get(TARGET));
                t.set(context, sb.toString());
            }
        }
    }

    @Override
    public String getHelpText() {
        return "Username generation using a hashed claim including the country of the user. If no country claim is found, then EL is used.";
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
	
}