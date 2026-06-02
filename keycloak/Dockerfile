# Imagen oficial de Keycloak — para despliegue en Render.com
FROM quay.io/keycloak/keycloak:24.0

# Modo desarrollo (start-dev) para simplificar la configuración
# En producción real usar: CMD ["start"]
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev", "--http-port=10000"]
