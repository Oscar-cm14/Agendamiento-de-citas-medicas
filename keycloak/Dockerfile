# Imagen oficial de Keycloak — para despliegue en Render.com
FROM quay.io/keycloak/keycloak:24.0

# Copiar el archivo de exportación del realm al directorio de importación de Keycloak
COPY clinica-realm-export.json /opt/keycloak/data/import/

# Modo desarrollo (start-dev) para simplificar la configuración
# Se añade --import-realm para cargar la configuración de clinica-realm en el arranque
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev", "--http-port=10000", "--import-realm"]
