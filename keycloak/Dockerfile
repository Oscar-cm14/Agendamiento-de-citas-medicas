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

# Copiar el archivo de exportación del realm al directorio de importación de Keycloak
COPY clinica-realm-export.json /opt/keycloak/data/import/

# Habilitar métricas y health
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Modo desarrollo (start-dev) para simplificar la configuración en Render
# Se añade --import-realm para cargar la configuración de clinica-realm en el arranque
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev", "--http-port=10000", "--import-realm"]
