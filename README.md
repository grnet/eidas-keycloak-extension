# Eidas Keycloak Extension

This repository contains a [keycloak](https://www.keycloak.org/) extension which adds support for the 
SAML v2.0 dialect of the [eIDAS](https://en.wikipedia.org/wiki/EIDAS) nodes.
It provides an identity provider extension which allows keycloak to be setup as an "identity broker". 

[Keycloak](https://www.keycloak.org/about) is an open source Identity and Access Management system for modern 
applications.

[eIDAS-Nodes](https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/eIDAS-Node+version+2.5) are operated 
from EU member states according to the eIDAS Regulation in order to ensure that people and businesses can use 
their own national eIDs (electronic identification schemes) to access public services available online in 
other countries.

The eIDAS Nodes use an extended version of SAML v2.0 which defines a number of SAML elements and attribute 
definitions which are not supported by default in standard SAML implementations. This extension provides support 
for these extensions, by offering a custom IdP which can use this extended dialect.

See [eIDAS+eID+Profile](https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/eIDAS+eID+Profile) and the 
following documents for the v1.2 technical specifications:

  * [eIDAS - Interoperability Architecture v1.2](https://ec.europa.eu/cefdigital/wiki/download/attachments/82773108/eIDAS%20Interoperability%20Architecture%20v.1.2%20Final.pdf)
  * [eIDAS - Cryptographic requirements for the Interoperability Framework v1.2](https://ec.europa.eu/cefdigital/wiki/download/attachments/82773108/eIDAS%20Cryptographic%20Requirement%20v.1.2%20Final.pdf)
  * [eIDAS SAML Message Format v1.2](https://ec.europa.eu/cefdigital/wiki/download/attachments/82773108/eIDAS%20SAML%20Message%20Format%20v.1.2%20Final.pdf)
  * [eIDAS SAML Attribute Profile v1.2](https://ec.europa.eu/cefdigital/wiki/download/attachments/82773108/eIDAS%20SAML%20Attribute%20Profile%20v1.2%20Final.pdf) 

# Installation 

Download the latest release jar from the releases page. Then deploy it in keycloak by copying it at folder
`KEYCLOAK_HOME/standalone/deployments/`. See the keycloak [documentation](https://www.keycloak.org/docs/latest/server_installation/index.html#distribution-directory-structure) for the directory structure of the keycloak server.

# Compatibility 


| Extension version | Keycloak version |
|-------------------|------------------|
| 0.5               | 15.0.2 - 18.0.2  |
| 0.6               | 18.0.2, 19.0.2 (partial admin UI) |
| 0.7               | 20.0.2 (partial admin UI) |
| 0.8               | 21.0.1 (partial admin UI) |
| 0.9               | 22.0.3 (partial admin UI) |
| 0.10              | 23.0.2 (partial admin UI) |
| 0.11              | 24.0.4 |

Depending on the version of keycloak (between 18 and 23) the admin UI might not show the extra attributes and you might need to configure the 
extension by editing the configuration inside the DB. 
Another possibility is to import your realm from json and thus be able to configure the extension. See this [example](howto/example.config.json) for an example.

# Providers 

The extension provides the following components which are needed in order to connect to an eIDAS node using 
the extended definitions of the eIDAS technical specifications: 

  * Identity provider "eIDAS SAML v2.0" which is an extended version of the default "SAML v2.0" IdP.
  * Mapper "Username Template Importer" which can be used to setup the ID or username for federated user lookup.
  * Mapper "Attribute Importer" which can be used to import additional attributes.
  * Authenticator "Citizen Country Selection" which can collect the citizen country before authentication. 

# Setup

  * Setup the keycloak realm key provider for signing requests according to the eIDAS specifications.
    Depending on the setup of the eIDAS node that you are trying to connect, it might be important that the 
    certificate contains the correct country code.
  * Add the "eIDAS SAML v2.0" identity provider. 
  * Setup the "eIDAS SAML v2.0" identity provider by setting the classic "SAML v2.0" options and the 
    additional eIDAS specific options.
  * Add a "Username Template Importer" with template something like `${ALIAS}.${ATTRIBUTE.PersonIdentifier}` and target `BROKER_ID`. 
    You can also adjust the username in a similar fashion. 
  * Add "Attribute Importer" for the attributes you want to consume, e.g. "DateOfBirth".
  * Go to "Authentication" and copy the "Browser" flow. 
  * After the "Cookie" execution add an "eIDAS" flow which contains the following two executions: 
     - Citizen Country Selection. Use the "Actions" menu to configure this by adjusting the available country codes. These codes 
       are two letter names.
     - Identity Provider Redirector. Use the "Actions" menu to adjust the "Default Identity Provider" to "eidas-saml", in order for
       the redirection to happen automatically.
  * Set the new flow as default in the "Browser Flow" bindings.
  * Adjust depending on your use case the "First Broker Login" and additional properties.

You can find a very simple howto guide at [howto/README.md](howto/README.md).

# Build and Install

Build the project using maven

```
mvn install
```

You can find the jar under `target/keycloak-eidas-idp-x.x.x.jar`.

# License 

Apache License, Version 2.0

