# ══════════════════════════════════════════════════════════════════════════
#  Keycloak 24 — Despliegue en Azure Container Instances
#
#  Variables de entorno requeridas al crear el contenedor en Azure:
#    KC_HOSTNAME          = URL pública del contenedor (sin https://)
#                           Ej: mi-keycloak.eastus.azurecontainer.io
#    KEYCLOAK_ADMIN       = admin
#    KEYCLOAK_ADMIN_PASSWORD = (contraseña segura)
# ══════════════════════════════════════════════════════════════════════════

FROM quay.io/keycloak/keycloak:24.0

# Habilitar métricas y health
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Base de datos interna (H2 dev) — suficiente para proyecto académico
# Para mayor persistencia, usar KC_DB=postgres con variables adicionales
ENV KC_DB=dev-file

# Puerto HTTP (Azure Container Instances no requiere HTTPS obligatorio en red interna)
EXPOSE 8080

# Arrancar en modo producción con HTTP habilitado
# KC_HOSTNAME se inyecta como variable de entorno desde Azure
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", \
            "--http-enabled=true", \
            "--hostname-strict=false", \
            "--hostname-strict-https=false"]
