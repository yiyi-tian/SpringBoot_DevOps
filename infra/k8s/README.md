# Kubernetes deployment for SpringBoot_DevOps microservices
#
# Prerequisites:
#   1. Build images: mvn clean package && docker build (see DEVELOPMENT.md)
#   2. cp secret-app.yaml.example secret-app.yaml && edit secrets
#   3. kubectl apply -f namespace.yaml
#   4. kubectl apply -f configmap-app.yaml
#   5. kubectl apply -f secret-app.yaml
#   6. kubectl apply -R -f mysql/ -f redis/ -f clickhouse/
#   7. kubectl apply -R -f user-service/ -f message-service/ -f log-service/ -f topbiz/
#   8. kubectl apply -f vector/deployment.yaml  (after creating vector ConfigMap from ../vector/vector.toml)
#   9. kubectl apply -f ingress.yaml
#
# Only topbiz is exposed via Ingress. Internal services use ClusterIP (8081-8083).
#
# Image tags default to devops/*:0.2.0-SNAPSHOT — push to your registry and update manifests for production.
