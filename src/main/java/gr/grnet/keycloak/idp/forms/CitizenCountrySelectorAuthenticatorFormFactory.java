package gr.grnet.keycloak.idp.forms;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class CitizenCountrySelectorAuthenticatorFormFactory implements AuthenticatorFactory {

	private static final String PROVIDER_ID = "auth-select-citizen-country";

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
		return false;
	}

	public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
			AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED };

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
		return null;
	}

	@Override
	public void close() {
		// NOOP
	}

	@Override
	public Authenticator create(KeycloakSession session) {
		return new CitizenCountrySelectorAuthenticatorForm();
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
