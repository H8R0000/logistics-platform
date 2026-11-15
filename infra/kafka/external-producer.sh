#!/usr/bin/env bash
set -euo pipefail

# Manual demo client: stdin contains sourceId:messageId, a tab and one-line JSON.
: "${KAFKA_EXTERNAL_PASSWORD:?Set KAFKA_EXTERNAL_PASSWORD}"
config=$(mktemp /tmp/logistics-external-producer.XXXXXX)
trap 'rm -f "$config"' EXIT
chmod 600 "$config"
password="${KAFKA_EXTERNAL_PASSWORD//\\/\\\\}"
password="${password//\"/\\\"}"
printf '%s\n' \
  'security.protocol=SASL_PLAINTEXT' \
  'sasl.mechanism=PLAIN' \
  "sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"partner-demo\" password=\"$password\";" \
  'acks=all' \
  'enable.idempotence=true' > "$config"

/opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:29092 \
  --producer.config "$config" --topic shipment-requests \
  --property parse.key=true --property "key.separator="$'\t'
