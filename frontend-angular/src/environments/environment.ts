// ============================================================
// environment.ts — Configuración para DESARROLLO LOCAL
// ============================================================
// Este archivo se usa cuando ejecutas: ng serve
// La URL apunta al backend local
// ============================================================

export const environment = {
  production: true,
  apiUrl: 'https://clinica-backend.onrender.com/api/v1',
  keycloakUrl: 'https://clinica-keycloak.onrender.com'
};
