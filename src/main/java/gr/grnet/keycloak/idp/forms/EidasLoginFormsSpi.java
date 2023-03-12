package gr.grnet.keycloak.idp.forms;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class EidasLoginFormsSpi implements Spi {

	@Override
	public boolean isInternal() {
		return false;
	}

	@Override
	public String getName() {
		return "eidas-freemarker";
	}

	@Override
	public Class<? extends Provider> getProviderClass() {
		return EidasLoginFormsProvider.class;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends ProviderFactory> getProviderFactoryClass() {
		return EidasLoginFormsProviderFactory.class;
	}

}
