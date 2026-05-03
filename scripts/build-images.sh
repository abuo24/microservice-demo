#!/usr/bin/env bash
# Build all service images and load them into your local Kubernetes cluster.
# Usage:
#   ./scripts/build-images.sh minikube     (default)
#   ./scripts/build-images.sh kind
#   ./scripts/build-images.sh k3s
#   ./scripts/build-images.sh docker       (Docker Desktop K8s — no load needed)

set -euo pipefail

CLUSTER="${1:-minikube}"

IMAGES=(
  "uz.coder/api-gateway:0.0.1-SNAPSHOT:api-gateway"
  "uz.coder/order-service:0.0.1-SNAPSHOT:order-service"
  "uz.coder/inventory-service:0.0.1-SNAPSHOT:inventory-service"
)

echo "==> Building JAR files..."
for entry in "${IMAGES[@]}"; do
  dir="${entry##*:}"
  echo "    Building $dir..."
  (cd "$dir" && ./gradlew bootJar --no-daemon -x test -q)
done

echo ""
echo "==> Building Docker images..."
for entry in "${IMAGES[@]}"; do
  image="${entry%%:*}"
  tag="${entry#*:}"; tag="${tag%:*}"
  dir="${entry##*:}"
  echo "    docker build $dir -> $image:$tag"
  docker build -t "$image:$tag" "./$dir"
done

echo ""
echo "==> Loading images into cluster: $CLUSTER"

case "$CLUSTER" in
  minikube)
    for entry in "${IMAGES[@]}"; do
      image="${entry%%:*}"
      tag="${entry#*:}"; tag="${tag%:*}"
      echo "    minikube image load $image:$tag"
      minikube image load "$image:$tag"
    done
    ;;

  kind)
    KIND_CLUSTER="${KIND_CLUSTER:-kind}"
    for entry in "${IMAGES[@]}"; do
      image="${entry%%:*}"
      tag="${entry#*:}"; tag="${tag%:*}"
      echo "    kind load docker-image $image:$tag --name $KIND_CLUSTER"
      kind load docker-image "$image:$tag" --name "$KIND_CLUSTER"
    done
    ;;

  k3s)
    for entry in "${IMAGES[@]}"; do
      image="${entry%%:*}"
      tag="${entry#*:}"; tag="${tag%:*}"
      echo "    k3s: saving and importing $image:$tag"
      docker save "$image:$tag" | sudo k3s ctr images import -
    done
    ;;

  docker)
    echo "    Docker Desktop shares the host Docker daemon — no load needed."
    ;;

  *)
    echo "Unknown cluster type: $CLUSTER"
    echo "Supported: minikube | kind | k3s | docker"
    exit 1
    ;;
esac

echo ""
echo "==> Done! Images loaded. You can now run:"
echo "    kubectl apply -f k8s/"
echo "    kubectl apply -f api-gateway/k8s/"
echo "    kubectl apply -f order-service/k8s/"
echo "    kubectl apply -f inventory-service/k8s/"