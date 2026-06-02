// ============================================================
// environment.prod.ts — Configuración para PRODUCCIÓN
// ============================================================
// Este archivo se usa cuando ejecutas: ng build --configuration production
// Cambia las URLs para apuntar a los servicios en la nube.
//
// IMPORTANTE: reemplaza las URLs con las que Railway te asigne:
//   - apiUrl:      URL de tu backend en Railway
//   - keycloakUrl: URL de tu Keycloak en Railway
// ============================================================

export const environment = {
  production: true,
  apiUrl:       'https://clinica-backend.up.railway.app/api/v1',
  keycloakUrl:  'https://clinica-keycloak.up.railway.app'
};
