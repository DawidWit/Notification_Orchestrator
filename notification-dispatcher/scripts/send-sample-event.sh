#!/usr/bin/env bash
# Publishes a sample notification decision to the local Kafka broker (from docker-compose).
#
# Usage:
#   ./scripts/send-sample-event.sh                # normal event  -> delivered (SENT)
#   ./scripts/send-sample-event.sh force_failure  # exhausts retries -> DEAD_LETTERED
#
# Requires the docker-compose stack to be running: `docker compose up -d`.
set -euo pipefail

EVENT_TYPE="${1:-security_alert}"
EVENT_ID="sample-$(date +%s)"
OCCURRED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

EVENT=$(printf '{"eventId":"%s","userId":"user-123","eventType":"%s","decision":"PROCESS_NOTIFICATION","channels":["EMAIL","SMS"],"occurredAt":"%s"}' \
  "$EVENT_ID" "$EVENT_TYPE" "$OCCURRED_AT")

echo "Publishing to topic 'notification.decisions':"
echo "  $EVENT"
echo "$EVENT" | docker exec -i dispatcher-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic notification.decisions

echo
echo "Published (eventId=$EVENT_ID). Inspect it via the REST API:"
echo "  curl -s http://localhost:8080/api/v1/deliveries/event/$EVENT_ID"
