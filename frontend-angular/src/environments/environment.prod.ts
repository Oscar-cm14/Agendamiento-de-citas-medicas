// ============================================================
// environment.prod.ts — Configuración PRODUCCIÓN
// Usado con: ng build --configuration production
//
// ⚠️  REEMPLAZA las URLs después de desplegar:
//   apiUrl      → URL de tu backend en Render
//   keycloakUrl → URL de tu Keycloak en Azure
// ============================================================

export const environment = {
  production: true,
  // Cambia esto a la URL de tu backend en Render
  apiUrl:       'https://clinica-backend-n6ft.onrender.com/api/v1',
  // Cambia esto a la URL de tu Keycloak en Render
  keycloakUrl:  'https://clinica-keycloak.onrender.com'
};
