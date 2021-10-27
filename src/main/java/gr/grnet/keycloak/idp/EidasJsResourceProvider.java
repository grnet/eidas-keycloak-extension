package gr.grnet.keycloak.idp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.services.resource.RealmResourceProvider;

public class EidasJsResourceProvider implements RealmResourceProvider {

	public static final String EIDAS_JS = "theme-resources/js/eidas.js";

	private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

	private String content;
	private boolean initialized;

	public EidasJsResourceProvider(KeycloakSession session) {
		this.initialized = false;
		this.content = null;
	}

	@Override
	public Object getResource() {
		return this;
	}

	@GET
	@Produces("text/javascript; charset=utf-8")
	public String get() {
		if (!initialized) {
			try (InputStream is = EidasJsResourceProvider.class.getClassLoader().getResourceAsStream(EIDAS_JS)) {
				content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
						.collect(Collectors.joining("\n"));
			} catch (Exception e) {
				logger.warn("Failed to read resource: " + EIDAS_JS);
				content = "";
			}
			initialized = true;
		}
		return content;
	}

	@Override
	public void close() {
	}

}
