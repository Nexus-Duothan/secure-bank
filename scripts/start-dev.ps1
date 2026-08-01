Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

Write-Host "Starting SecureBank local development stack..."
docker start securebank-postgres | Out-Null

$services = @(
  @{ Name = "auth-service"; Path = "backend/auth-service"; Port = 8081 },
  @{ Name = "totp-service"; Path = "backend/totp-service"; Port = 8082 },
  @{ Name = "user-service"; Path = "backend/user-service"; Port = 8083 },
  @{ Name = "accounts-service"; Path = "backend/accounts-service"; Port = 8084 },
  @{ Name = "transfer-service"; Path = "backend/transfer-service"; Port = 8085 },
  @{ Name = "payments-service"; Path = "backend/payments-service"; Port = 8086 },
  @{ Name = "lending-service"; Path = "backend/lending-service"; Port = 8087 },
  @{ Name = "notification-service"; Path = "backend/notification-service"; Port = 8088 },
  @{ Name = "api-gateway"; Path = "backend/api-gateway"; Port = 8080 }
)

foreach ($service in $services) {
  Write-Host ("Launching {0} on port {1}..." -f $service.Name, $service.Port)
  Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "cd '$repoRoot'; mvn -pl $($service.Path) spring-boot:run"
  ) -WorkingDirectory $repoRoot
}

Write-Host "Launching frontend on port 5173..."
Start-Process powershell -ArgumentList @(
  "-NoExit",
  "-Command",
  "cd '$repoRoot/frontend/web-service'; npm run dev"
) -WorkingDirectory (Join-Path $repoRoot "frontend/web-service")

Write-Host "All services launched. Open http://localhost:5173"
