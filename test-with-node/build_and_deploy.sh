#!/bin/bash
pushd ..
mvn clean package
cp target/keycloak-eidas-idp-*.jar test-with-node/etc/keycloak/deployments/
popd
docker-compose up
