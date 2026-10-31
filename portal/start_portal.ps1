# Starts the portal against the shared Azure SQL database.
# Asks for the Azure login each time; nothing secret is stored in this file.
#
#   .\start_portal.ps1            just run the site
#   .\start_portal.ps1 -Migrate   apply new database changes first (after pulling new migrations)

param([switch]$Migrate)

Set-Location $PSScriptRoot
. .\venv\Scripts\Activate.ps1

$env:DB_ENGINE = "mssql"
$env:DB_HOST   = "plantdex-fyp15-ea.database.windows.net"
$env:DB_NAME   = "PlantDexDB"
$env:DB_DRIVER = "ODBC Driver 18 for SQL Server"
$env:DB_USER   = Read-Host "Azure admin login"
$env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR((Read-Host "Azure password" -AsSecureString)))

if ($Migrate) {
    python manage.py migrate
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Migrate failed. If it timed out, the database was asleep: wait 30 s and run again." -ForegroundColor Yellow
        exit 1
    }
}

python manage.py runserver
