#!/usr/bin/env bash
set -euo pipefail

: "${KAFKA_ADMIN_PASSWORD:?Set KAFKA_ADMIN_PASSWORD}"
config="${1:-/tmp/logistics-kafka-admin.properties}"
password="${KAFKA_ADMIN_PASSWORD//\\/\\\\}"
password="${password//\"/\\\"}"
umask 077
printf '%s\n' \
  'security.protocol=SASL_PLAINTEXT' \
  'sasl.mechanism=PLAIN' \
  "sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"$password\";" \
  > "$config"
