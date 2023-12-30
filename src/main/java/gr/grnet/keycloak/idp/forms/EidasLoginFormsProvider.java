package gr.grnet.keycloak.idp.forms;

import org.keycloak.provider.Provider;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public interface EidasLoginFormsProvider extends Provider {
	
    Response createEidasSamlPostForm();

    EidasLoginFormsProvider setFormData(MultivaluedMap<String, String> formData);
    
}
