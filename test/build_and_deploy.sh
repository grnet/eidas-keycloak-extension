#!/bin/bash
cd ..
mvn clean package
cp target/keycloak-eidas-idp-*.jar test/deployments/
cd test/
docker-compose up
