#!/bin/bash
pushd ..
mvn clean package
cp target/keycloak-eidas-idp-*.jar test/deployments/
popd
docker-compose up
