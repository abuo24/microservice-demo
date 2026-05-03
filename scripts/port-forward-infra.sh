#!/usr/bin/env bash
# Forward all infrastructure services from Kubernetes to localhost.
# Run this once, then start your Spring Boot apps with SPRING_PROFILES_ACTIVE=local
#
# Usage: ./scripts/port-forward-infra.sh [namespace]
#   namespace defaults to "microservices"

set -euo pipefail

NS="${1:-microservices}"
PIDS=()

cleanup() {
  echo ""
  echo "==> Stopping all port-forwards..."
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  echo "    Done."
}
trap cleanup EXIT INT TERM

wait_for_pod() {
  local label="$1"
  echo "    Waiting for pod: $label"
  kubectl wait --for=condition=ready pod -l "app=$label" -n "$NS" --timeout=120s
}

echo "==> Waiting for infra pods to be ready in namespace: $NS"
wait_for_pod postgres
wait_for_pod redis
wait_for_pod kafka
wait_for_pod zipkin
wait_for_pod keycloak

echo ""
echo "==> Starting port-forwards..."

kubectl port-forward -n "$NS" svc/postgres  5432:5432  &> /tmp/pf-postgres.log  & PIDS+=($!)
echo "    postgres  → localhost:5432"

kubectl port-forward -n "$NS" svc/redis     6379:6379  &> /tmp/pf-redis.log     & PIDS+=($!)
echo "    redis     → localhost:6379"

kubectl port-forward -n "$NS" svc/kafka     9092:9092  &> /tmp/pf-kafka.log     & PIDS+=($!)
echo "    kafka     → localhost:9092"

kubectl port-forward -n "$NS" svc/zipkin    9411:9411  &> /tmp/pf-zipkin.log    & PIDS+=($!)
echo "    zipkin    → localhost:9411"

kubectl port-forward -n "$NS" svc/keycloak  8090:8080  &> /tmp/pf-keycloak.log  & PIDS+=($!)
echo "    keycloak  → localhost:8090  (Admin UI)"

echo ""
echo "==> IMPORTANT — add this line to /etc/hosts if not already present:"
echo "    127.0.0.1   kafka"
echo ""
echo "    Run: sudo sh -c \"echo '127.0.0.1 kafka' >> /etc/hosts\""
echo ""
echo "==> Use these env vars for your Spring Boot apps:"
echo ""
echo "    SPRING_PROFILES_ACTIVE=local"
echo "    SPRING_CLOUD_KUBERNETES_ENABLED=false"
echo "    DB_HOST=localhost"
echo "    DB_PORT=5432"
echo "    REDIS_HOST=localhost"
echo "    REDIS_PORT=6379"
echo "    REDIS_PASSWORD=redisPass"
echo "    KAFKA_BOOTSTRAP_SERVERS=kafka:9092"
echo "    JWT_ISSUER_URI=http://localhost:8090/realms/microservices"
echo "    JWT_JWK_SET_URI=http://localhost:8090/realms/microservices/protocol/openid-connect/certs"
echo "    ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans"
echo ""
echo "==> All port-forwards running. Press Ctrl+C to stop."
echo ""

# Keep script alive and show logs if a forward dies
while true; do
  for pid in "${PIDS[@]}"; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "WARN: port-forward pid=$pid died — check logs in /tmp/pf-*.log"
    fi
  done
  sleep 10
done