# Eidas Keycloak Extension

This repository contains a keycloak extension which adds support for the SAML v2.0 dialect of the eIDAS nodes.
It provides an identity provider extension which allows keycloak to setup as an "identity broker". 

# Build

Build the project using maven

```
mvn clean install
```

# Installation 

Copy the jar file located at `target/` into the keycloak deployments folder.

# License 

Apache License, Version 2.0

