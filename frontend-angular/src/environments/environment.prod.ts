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
  apiUrl:      'https://agendamiento-de-citas-medicas.onrender.com/api/v1',
  keycloakUrl: 'https://REEMPLAZAR-CON-TU-URL-AZURE.azurecontainer.io'
};
