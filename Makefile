NS = microservices

# ── Skaffold (any cluster) ──────────────────────────────────────
.PHONY: run dev infra-only
run:
	skaffold run

dev:
	skaffold dev

infra-only:
	skaffold run --profile infra-only

# ── minikube shortcut (no extra tools) ─────────────────────────
.PHONY: minikube-run minikube-env-unset
minikube-run:
	eval $$(minikube docker-env) && \
	docker compose build && \
	kubectl apply -f k8s/namespace.yaml && \
	kubectl apply -f k8s/ && \
	kubectl apply -f api-gateway/k8s/ && \
	kubectl apply -f order-service/k8s/ && \
	kubectl apply -f inventory-service/k8s/

# ── Cleanup ─────────────────────────────────────────────────────
.PHONY: delete
delete:
	kubectl delete namespace $(NS) --ignore-not-found

.PHONY: logs
logs:
	kubectl logs -n $(NS) -l app=api-gateway --tail=50 &
	kubectl logs -n $(NS) -l app=order-service --tail=50 &
	kubectl logs -n $(NS) -l app=inventory-service --tail=50

.PHONY: status
status:
	kubectl get pods -n $(NS)