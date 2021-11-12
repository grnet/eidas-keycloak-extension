
# How to generate

The keystore in this directory contains a keystore which can be used to connect to the eIDAS node. It was generated using 
the following commands.


```
keytool -genkey -keyalg RSA -alias selfsigned -keystore keycloak.jks -storepass local-demo -validity 3600 -keysize 4096
What is your first and last name?
  [Unknown]:  keycloak
What is the name of your organizational unit?
  [Unknown]:  keycloak
What is the name of your organization?
  [Unknown]:  keycloak
What is the name of your City or Locality?
  [Unknown]:  keycloak
What is the name of your State or Province?
  [Unknown]:  keycloak
What is the two-letter country code for this unit?
  [Unknown]:  CA
Is CN=keycloak, OU=keycloak, O=keycloak, L=keycloak, ST=keycloak, C=CA correct?
  [no]:  yes

Enter key password for <selfsigned>
	(RETURN if same as keystore password):  

local-demo

Re-enter new password: 

local-demo

Warning:
The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12 which is an industry standard format using "keytool -importkeystore -srckeystore keycloak.jks -destkeystore keycloak.jks -deststoretype pkcs12".
```

# How to load in keycloak 

keystore: /opt/keycloak.jks
keystore password: local-demo
key alias: selfsigned
key password: local-demo

