.PHONY: help minikube-lightweight lightweight-up lightweight-down lightweight-infra-up lightweight-infra-down lightweight-svcs-up lightweight-svcs-down dev-up dev-down logs-orders logs-inventory logs-gateway logs-keycloak dev-lightweight dev-lightweight-svcs fast-lightweight-up fast-dev-lightweight

help:
	@echo "Lightweight Development Setup"
	@echo ""
	@echo "Profiles:"
	@echo "  make lightweight-up         - Full lightweight (postgres + kafka + keycloak + services)"
	@echo "  make lightweight-infra-up   - Infra only (postgres + kafka + keycloak)"
	@echo "  make lightweight-svcs-up    - Services only (api-gateway + order + inventory)"
	@echo ""
	@echo "Fast deployment (with cache):"
	@echo "  make fast-lightweight-up    - Fast full lightweight (cached builds)"
	@echo "  make fast-dev-lightweight   - Fast dev mode with hot reload"
	@echo ""
	@echo "Teardown:"
	@echo "  make lightweight-down       - Remove lightweight deployment"
	@echo "  make lightweight-infra-down - Remove infra"
	@echo "  make lightweight-svcs-down  - Remove services"
	@echo ""
	@echo "Other:"
	@echo "  make minikube-lightweight   - Start minikube with minimal resources"
	@echo "  make dev-up                 - Deploy full stack (all infra)"
	@echo "  make dev-down               - Remove full stack"
	@echo "  make logs-orders            - Stream order-service logs"
	@echo "  make logs-inventory         - Stream inventory-service logs"

minikube-lightweight:
	minikube start --cpus=2 --memory=4096 --driver=docker

lightweight-up:
	@echo "Deploying lightweight stack (postgres + kafka + keycloak + services)..."
	skaffold run -p lightweight --port-forward

lightweight-down:
	skaffold delete -p lightweight

lightweight-infra-up:
	@echo "Deploying lightweight infra (postgres + kafka + keycloak)..."
	skaffold run -p lightweight-infra-only --port-forward

lightweight-infra-down:
	skaffold delete -p lightweight-infra-only

lightweight-svcs-up:
	@echo "Deploying lightweight services (api-gateway + order + inventory)..."
	skaffold run -p lightweight-services-only --port-forward

lightweight-svcs-down:
	skaffold delete -p lightweight-services-only

dev-up:
	@echo "Deploying full stack (all infrastructure)..."
	skaffold run --port-forward

dev-down:
	skaffold delete

logs-orders:
	kubectl logs -f -n microservices deployment/order-service

logs-inventory:
	kubectl logs -f -n microservices deployment/inventory-service

logs-gateway:
	kubectl logs -f -n microservices deployment/api-gateway

logs-keycloak:
	kubectl logs -f -n microservices deployment/keycloak

# Development mode - rebuild on changes
dev-lightweight:
	skaffold dev -p lightweight --port-forward

dev-lightweight-svcs:
	skaffold dev -p lightweight-services-only --port-forward

# Fast builds (skip tests, use cache)
fast-lightweight-up:
	@echo "Fast deployment (cache enabled, parallel builds)..."
	skaffold run -p lightweight --port-forward --cache-artifacts

fast-dev-lightweight:
	@echo "Fast dev mode (auto-rebuild with cache)..."
	skaffold dev -p lightweight --port-forward --cache-artifacts