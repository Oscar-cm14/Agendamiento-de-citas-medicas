$BASE = "https://clinica-keycloak.onrender.com"

Write-Host "1. Obteniendo token..." -ForegroundColor Cyan
$tokenResponse = Invoke-RestMethod -Method Post `
    -Uri "$BASE/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "client_id=admin-cli&grant_type=password&username=admin&password=admin123"
$TOKEN = $tokenResponse.access_token
$headers = @{ Authorization = "Bearer $TOKEN"; "Content-Type" = "application/json" }
Write-Host "Token OK" -ForegroundColor Green

Write-Host "2. Creando realm clinica-realm..." -ForegroundColor Cyan
try {
    Invoke-RestMethod -Method Post -Uri "$BASE/admin/realms" -Headers $headers `
        -Body '{"realm":"clinica-realm","enabled":true,"displayName":"Clinica Piedra Azul"}'
    Write-Host "Realm creado" -ForegroundColor Green
} catch { Write-Host "Realm ya existe (OK)" -ForegroundColor Yellow }

Write-Host "3. Creando cliente clinica-frontend..." -ForegroundColor Cyan
try {
    Invoke-RestMethod -Method Post -Uri "$BASE/admin/realms/clinica-realm/clients" -Headers $headers `
        -Body '{"clientId":"clinica-frontend","enabled":true,"publicClient":true,"directAccessGrantsEnabled":true,"redirectUris":["*"],"webOrigins":["*"]}'
    Write-Host "Cliente creado" -ForegroundColor Green
} catch { Write-Host "Cliente ya existe (OK)" -ForegroundColor Yellow }

Write-Host "4. Creando roles..." -ForegroundColor Cyan
foreach ($role in @("ADMIN","PATIENT","DOCTOR","SCHEDULER")) {
    try {
        Invoke-RestMethod -Method Post -Uri "$BASE/admin/realms/clinica-realm/roles" -Headers $headers `
            -Body "{`"name`":`"$role`"}"
        Write-Host "Rol $role creado" -ForegroundColor Green
    } catch { Write-Host "Rol $role ya existe (OK)" -ForegroundColor Yellow }
}

Write-Host "5. Creando usuario admin..." -ForegroundColor Cyan
try {
    Invoke-RestMethod -Method Post -Uri "$BASE/admin/realms/clinica-realm/users" -Headers $headers `
        -Body '{"username":"admin","enabled":true,"email":"admin@clinica.com","credentials":[{"type":"password","value":"admin123","temporary":false}]}'
    Write-Host "Usuario admin creado" -ForegroundColor Green
} catch { Write-Host "Usuario admin ya existe (OK)" -ForegroundColor Yellow }

Write-Host "6. Obteniendo ID del usuario admin..." -ForegroundColor Cyan
$users = Invoke-RestMethod -Method Get -Uri "$BASE/admin/realms/clinica-realm/users?username=admin" -Headers $headers
$userId = $users[0].id
Write-Host "User ID: $userId" -ForegroundColor Green

Write-Host "7. Obteniendo ID del rol ADMIN..." -ForegroundColor Cyan
$roles = Invoke-RestMethod -Method Get -Uri "$BASE/admin/realms/clinica-realm/roles" -Headers $headers
$adminRole = $roles | Where-Object { $_.name -eq "ADMIN" }
Write-Host "Role ID: $($adminRole.id)" -ForegroundColor Green

Write-Host "8. Asignando rol ADMIN al usuario admin..." -ForegroundColor Cyan
$roleBody = "[{`"id`":`"$($adminRole.id)`",`"name`":`"ADMIN`"}]"
Invoke-RestMethod -Method Post -Uri "$BASE/admin/realms/clinica-realm/users/$userId/role-mappings/realm" -Headers $headers -Body $roleBody
Write-Host "Rol asignado!" -ForegroundColor Green

Write-Host "9. Reseteando contraseña del usuario admin..." -ForegroundColor Cyan
Invoke-RestMethod -Method Put -Uri "$BASE/admin/realms/clinica-realm/users/$userId/reset-password" -Headers $headers `
    -Body '{"type":"password","value":"admin123","temporary":false}'
Write-Host "Contraseña reseteada OK" -ForegroundColor Green

Write-Host "`n✅ Keycloak configurado exitosamente!" -ForegroundColor Green
Write-Host "Realm: clinica-realm" -ForegroundColor White
Write-Host "Usuario: admin / Contraseña: admin123" -ForegroundColor White
