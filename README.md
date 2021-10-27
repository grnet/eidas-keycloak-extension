# Eidas Keycloak Extension

An identity provider extension which supports brokering identity to an eIDAS node.

# Build

You can build the project using maven (ex mvn clean package) and then deploy the jar file to a keycloak installation. 

# Development

There is a docker-compose file inside the ./test folder. You may test the module by coping the Jar file to the ./test/deployments folder and then start keycloak using docker.

# Notes

On startup, keycloak will cache all UI relevant component (themes,extentions etc). See [this](https://wjw465150.gitbooks.io/keycloak-documentation/content/server_installation/topics/cache/disable.html) documentation in order to disable cache or simply do a `docker-compose rm keycloak` to drop the container and clean all caches 