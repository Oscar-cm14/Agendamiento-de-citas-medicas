@echo off
echo Obteniendo token de Keycloak...

curl -s -X POST https://clinica-keycloak.onrender.com/realms/master/protocol/openid-connect/token ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "client_id=admin-cli&grant_type=password&username=admin&password=admin123" ^
  -o token_response.json

for /f "usebackq tokens=*" %%a in (`powershell -Command "(Get-Content token_response.json | ConvertFrom-Json).access_token"`) do set TOKEN=%%a

echo Token obtenido.

echo Creando realm clinica-realm...
curl -s -X POST https://clinica-keycloak.onrender.com/admin/realms ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"realm\":\"clinica-realm\",\"enabled\":true,\"displayName\":\"Clinica Piedra Azul\"}"

echo.
echo Creando cliente clinica-frontend...
curl -s -X POST https://clinica-keycloak.onrender.com/admin/realms/clinica-realm/clients ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"clientId\":\"clinica-frontend\",\"enabled\":true,\"publicClient\":true,\"directAccessGrantsEnabled\":true,\"redirectUris\":[\"*\"],\"webOrigins\":[\"*\"]}"

echo.
echo Creando rol ADMIN en clinica-realm...
curl -s -X POST https://clinica-keycloak.onrender.com/admin/realms/clinica-realm/roles ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"ADMIN\"}"

echo.
echo Creando usuario admin en clinica-realm...
curl -s -X POST https://clinica-keycloak.onrender.com/admin/realms/clinica-realm/users ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"enabled\":true,\"credentials\":[{\"type\":\"password\",\"value\":\"admin123\",\"temporary\":false}]}"

echo.
echo Configuracion completada!
del token_response.json
pause
