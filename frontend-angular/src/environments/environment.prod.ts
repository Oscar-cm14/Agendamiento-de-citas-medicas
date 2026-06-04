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
  // Cambia esto a la URL de tu backend en Render
  apiUrl:       'https://clinica-backend.onrender.com/api/v1',
  // Cambia esto a la URL de tu Keycloak en Render
  keycloakUrl:  'https://clinica-keycloak.onrender.com'
};
