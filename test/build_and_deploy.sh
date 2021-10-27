#!/bin/bash
cd ..
mvn clean package
cp target/keycloak-eidas-idp-0.0.1.jar test/deployments/
cd test/
docker-compose up