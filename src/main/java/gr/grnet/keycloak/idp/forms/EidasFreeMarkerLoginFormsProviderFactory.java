package gr.grnet.keycloak.idp.forms;

import org.keycloak.Config;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.LoginFormsProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.FreeMarkerUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EidasFreeMarkerLoginFormsProviderFactory implements LoginFormsProviderFactory {

	private FreeMarkerUtil freeMarker;

	@Override
	public LoginFormsProvider create(KeycloakSession session) {
		return new EidasFreeMarkerLoginFormsProvider(session, freeMarker);
	}

	@Override
	public void init(Config.Scope config) {
		freeMarker = new FreeMarkerUtil();
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {

	}

	@Override
	public void close() {
		freeMarker = null;
	}

	@Override
	public String getId() {
		return "freemarker";
	}

}
