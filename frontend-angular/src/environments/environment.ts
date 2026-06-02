// ============================================================
// environment.ts — Configuración para DESARROLLO LOCAL
// ============================================================
// Este archivo se usa cuando ejecutas: ng serve
// La URL apunta al backend local
// ============================================================

export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  keycloakUrl: 'http://localhost:8081'
};
