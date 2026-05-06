# Guía de Inicio Rápido (Backend con Keycloak)

Se ha integrado **Keycloak** para manejar la autenticación y autorización mediante JWT. 

Para que la aplicación funcione en tu entorno local, debes seguir estos pasos obligatoriamente:

## 1. Levantar Keycloak Localmente
Asegúrate de tener Docker instalado y ejecutándose. Abre una terminal en la raíz de este proyecto (backend-reservas) y ejecuta:
```
docker-compose up -d
```
Esto levantará un contenedor de Keycloak en el puerto `8081`.

## 2. Importar la Configuración del Realm
La configuración de clientes y roles no se sube automáticamente porque Docker usa un volumen local. Por eso, se ha exportado la configuración en el archivo `clinica-realm-export.json`.

1. Abre tu navegador y ve a `http://localhost:8081`
2. Haz clic en **Administration Console** e inicia sesión con las credenciales por defecto:
   - **User:** `admin`
   - **Password:** `admin`
3. En la esquina superior izquierda, haz clic en el botón desplegable que dice **"master"** y selecciona **"Create Realm"**.
4. En la pantalla que aparece, NO escribas un nombre todavía. Haz clic en el botón **"Browse..."** (o arrastra el archivo) y selecciona el archivo `clinica-realm-export.json` que está en la raíz del repositorio.
5. Haz clic en **"Create"**. 

¡Listo! Keycloak ya tiene el cliente de Angular, los roles y la configuración de JWT listos para funcionar.

## 3. Arrancar los Servicios
- **Backend:** Ve a la carpeta `backend-reservas` y ejecuta `mvn spring-boot:run -pl app-main`.
- **Frontend:** Ve a la carpeta `frontend-angular` y ejecuta `npm start`.

*Nota:* Si el archivo `.json` de exportación incluía usuarios, puedes iniciar sesión inmediatamente. Si no, tendrás que crear usuarios de prueba en Keycloak (pestaña Users) y asignarles los roles (`ADMIN`, `DOCTOR`, etc.) en la pestaña "Role Mapping".
Crear Usuarios de Prueba: Ve a la sección Users y crea los usuarios necesarios. En la pestaña Credentials, asígnales una contraseña asegurándote de desmarcar la opción "Temporary". Finalmente, en la pestaña Role Mapping, asígnales el rol que les corresponda.