package gr.grnet.keycloak.idp.forms;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.provider.Provider;

public interface EidasLoginFormsProvider extends Provider {
	
    Response createEidasSamlPostForm();

    EidasLoginFormsProvider setFormData(MultivaluedMap<String, String> formData);
    
}
