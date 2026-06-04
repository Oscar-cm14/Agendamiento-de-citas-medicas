# Guía de Despliegue — Clínica Piedra Azul

## Arquitectura
| Componente | Plataforma | Plan |
|-----------|-----------|------|
| Keycloak  | Azure Container Instances (Azure for Students) | Gratis ($100 crédito) |
| Backend   | Render | Free |
| Frontend  | Vercel | Free |
| PostgreSQL| Render | Free |

---

## PASO 1 — Keycloak en Azure

### 1.1 Crear cuenta Azure for Students
1. Ve a https://azure.microsoft.com/es-es/free/students/
2. Regístrate con tu correo institucional de la Unicauca
3. Obtienes **$100 de crédito** sin tarjeta de crédito

### 1.2 Desplegar Keycloak con Azure Container Instances
1. En el portal de Azure (portal.azure.com) → **Crear un recurso** → busca **Container Instances**
2. Configura:
   - **Nombre del contenedor**: `clinica-keycloak`
   - **Región**: East US (la más económica)
   - **Imagen**: `quay.io/keycloak/keycloak:24.0`
   - **Puertos**: 8080
   - **CPU**: 1 vCPU / **Memoria**: 1.5 GB
3. En **Variables de entorno** agrega:
   ```
   KEYCLOAK_ADMIN          = admin
   KEYCLOAK_ADMIN_PASSWORD = (contraseña segura, guárdala)
   KC_DB                   = dev-file
   KC_HOSTNAME_STRICT      = false
   KC_HTTP_ENABLED         = true
   ```
4. En **Comando de inicio** (override):
   ```
   /opt/keycloak/bin/kc.sh start --http-enabled=true --hostname-strict=false --hostname-strict-https=false
   ```
5. Haz clic en **Revisar y crear** → **Crear**
6. Espera ~3 minutos. Copia la **IP pública** que aparece en el recurso.
   - Tu URL de Keycloak será: `http://<IP-PUBLICA>:8080`

### 1.3 Configurar el Realm en Keycloak
1. Ve a `http://<IP-PUBLICA>:8080/admin` → inicia sesión con `admin` / tu contraseña
2. **Crea el realm**:
   - Clic en el menú desplegable "master" → **Create realm**
   - Nombre: `clinica-realm` → **Create**
3. **Crea los roles del realm** (Realm roles → Create role):
   - `ADMIN`
   - `DOCTOR`
   - `SCHEDULER`
   - `PATIENT`
4. **Crea el cliente**:
   - Clients → **Create client**
   - Client ID: `clinica-frontend`
   - Client Protocol: `openid-connect`
   - Access Type: `public`
   - Valid Redirect URIs: `https://agendamiento-de-citas-medicas.vercel.app/*`
   - Web Origins: `https://agendamiento-de-citas-medicas.vercel.app`
   - **Save**
5. **Crea el usuario admin de la clínica**:
   - Users → **Add user**
   - Username: `admin`
   - Email: `admin@clinica.com`
   - → **Create** → pestaña **Credentials** → Set Password (no temporal)
   - → pestaña **Role mapping** → asigna rol `ADMIN`

> ⚠️ **Importante**: Anota la URL `http://<IP-PUBLICA>:8080`
> La necesitarás para configurar el backend en Render.

---

## PASO 2 — Backend en Render

### 2.1 Subir el código a GitHub
Si no tienes el proyecto en GitHub todavía:
```bash
cd backend-reservas
git init
git add .
git commit -m "deploy: configuración para producción"
git remote add origin https://github.com/TU-USUARIO/clinica-backend.git
git push -u origin main
```

### 2.2 Crear el servicio en Render
1. Ve a https://render.com → inicia sesión
2. **New** → **Blueprint** → conecta tu repositorio de GitHub
3. Render detectará el archivo `render.yaml` automáticamente
4. Se crearán dos recursos:
   - `clinica-db` (PostgreSQL)
   - `agendamiento-de-citas-medicas` (Web Service)

### 2.3 Completar las variables de entorno secretas
En Render → tu web service → **Environment**, agrega las variables con `sync: false`:

| Variable | Valor |
|----------|-------|
| `KEYCLOAK_SERVER_URL` | `http://<IP-PUBLICA-AZURE>:8080` |
| `KEYCLOAK_ISSUER_URI` | `http://<IP-PUBLICA-AZURE>:8080/realms/clinica-realm` |
| `KEYCLOAK_ADMIN_PASSWORD` | La contraseña que pusiste en Azure |
| `SPRING_MAIL_USERNAME` | tu-correo@gmail.com |
| `SPRING_MAIL_PASSWORD` | contraseña de aplicación de Gmail |
| `TWILIO_ACCOUNT_SID` | (de tu cuenta Twilio) |
| `TWILIO_AUTH_TOKEN` | (de tu cuenta Twilio) |

### 2.4 Verificar el despliegue
- El primer build tarda ~10 minutos (descarga Maven + compila)
- URL final: `https://agendamiento-de-citas-medicas.onrender.com`
- Verifica: `https://agendamiento-de-citas-medicas.onrender.com/actuator/health`
  - Debe responder `{"status":"UP"}`

> ⚠️ **Plan Free de Render**: el servicio se "duerme" tras 15 min de inactividad.
> El primer request después de estar dormido tarda ~30 segundos.

---

## PASO 3 — Frontend en Vercel

### 3.1 Actualizar la URL de Azure en el código
Antes de subir a Vercel, edita `frontend-angular/src/environments/environment.prod.ts`:
```typescript
export const environment = {
  production: true,
  apiUrl:      'https://agendamiento-de-citas-medicas.onrender.com/api/v1',
  keycloakUrl: 'http://<IP-PUBLICA-AZURE>:8080'  // ← tu IP real de Azure
};
```

### 3.2 Subir el frontend a GitHub
```bash
cd frontend-angular
git init
git add .
git commit -m "deploy: configuración para Vercel"
git remote add origin https://github.com/TU-USUARIO/clinica-frontend.git
git push -u origin main
```

### 3.3 Desplegar en Vercel
1. Ve a https://vercel.com → inicia sesión
2. **Add New Project** → importa tu repositorio de GitHub
3. Vercel detecta el `vercel.json` automáticamente
4. Haz clic en **Deploy**
5. URL final: `https://agendamiento-de-citas-medicas.vercel.app`

---

## PASO 4 — Actualizar CORS en el Backend (si Vercel asigna otra URL)

Si Vercel te asignó una URL diferente a `agendamiento-de-citas-medicas.vercel.app`:

En Render → tu web service → **Environment**:
```
FRONTEND_URL = https://TU-URL-REAL.vercel.app
```

Render reiniciará el servicio automáticamente.

---

## Resumen de URLs finales

| Servicio | URL |
|----------|-----|
| Keycloak Admin | `http://<IP-AZURE>:8080/admin` |
| Backend API | `https://agendamiento-de-citas-medicas.onrender.com/api/v1` |
| Frontend | `https://agendamiento-de-citas-medicas.vercel.app` |
| H2 Console (solo dev local) | `http://localhost:8080/h2-console` |

---

## Notas importantes

- **Azure for Students**: la IP pública de Container Instances puede cambiar si reinicias el contenedor. Considera asignar una **IP estática** o usar un **DNS name label** en Azure (está en la configuración de red del contenedor).
- **Render free**: la base de datos PostgreSQL gratuita expira a los **90 días**. Después necesitarás crear una nueva.
- **Keycloak y reinicios**: si el contenedor de Azure se reinicia, los datos de Keycloak (usuarios, realm) se pierden porque usa `dev-file` sin volumen persistente. Para producción real, monta un volumen Azure File Share.
