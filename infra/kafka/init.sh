#!/usr/bin/env bash
set -euo pipefail

config=/tmp/logistics-kafka-admin.properties
bash /etc/kafka/logistics/admin-config.sh "$config"

for topic in shipment-requests shipment-processing shipment-events shipment-errors shipment-dlq; do
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --command-config "$config" \
    --create --if-not-exists --topic "$topic" --partitions 3 --replication-factor 1
done

acl() {
  /opt/kafka/bin/kafka-acls.sh --bootstrap-server kafka:29092 --command-config "$config" --add "$@"
}

acl --allow-principal User:shipment-management --operation Write --operation Describe --topic shipment-processing
acl --allow-principal User:shipment-management --operation Read --operation Describe --topic shipment-events
acl --allow-principal User:shipment-management --operation Read --operation Describe --topic shipment-requests
acl --allow-principal User:shipment-management --operation Read --operation Describe --group shipment-management
acl --allow-principal User:shipment-management --operation Write --operation Describe --topic shipment-dlq
acl --allow-principal User:shipment-management --operation IdempotentWrite --cluster

acl --allow-principal User:shipment-processing --operation Read --operation Describe --topic shipment-processing
acl --allow-principal User:shipment-processing --operation Read --operation Describe --group shipment-processing
acl --allow-principal User:shipment-processing --operation Write --operation Describe --topic shipment-events
acl --allow-principal User:shipment-processing --operation Write --operation Describe --topic shipment-errors
acl --allow-principal User:shipment-processing --operation Write --operation Describe --topic shipment-dlq
acl --allow-principal User:shipment-processing --operation IdempotentWrite --cluster

acl --allow-principal User:partner-demo --operation Write --operation Describe --topic shipment-requests
acl --allow-principal User:partner-demo --operation IdempotentWrite --cluster
