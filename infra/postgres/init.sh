#!/bin/sh
set -eu

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 \
    --set management_password="$MANAGEMENT_DB_PASSWORD" \
    --set processing_password="$PROCESSING_DB_PASSWORD" \
    --set courier_password="$COURIER_DB_PASSWORD" \
    --set keycloak_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
CREATE USER shipment_management WITH PASSWORD :'management_password';
CREATE DATABASE shipment_management OWNER shipment_management;
REVOKE ALL ON DATABASE shipment_management FROM PUBLIC;

CREATE USER shipment_processing WITH PASSWORD :'processing_password';
CREATE DATABASE shipment_processing OWNER shipment_processing;
REVOKE ALL ON DATABASE shipment_processing FROM PUBLIC;

CREATE USER courier_provider WITH PASSWORD :'courier_password';
CREATE DATABASE courier_provider OWNER courier_provider;
REVOKE ALL ON DATABASE courier_provider FROM PUBLIC;

CREATE USER keycloak WITH PASSWORD :'keycloak_password';
CREATE DATABASE keycloak OWNER keycloak;
REVOKE ALL ON DATABASE keycloak FROM PUBLIC;
SQL
