# Starts the portal against the shared Azure SQL database.
# Asks for the Azure login each time; nothing secret is stored in this file.
#
#   .\start_portal.ps1            just run the site
#   .\start_portal.ps1 -Migrate   apply new database changes first (after pulling new migrations)
#   .\start_portal.ps1 -Migrate -SeedScanCards -Photos C:\path\to\dataset
#                                 also create one collectible per PlantDex model class (safe to repeat);
#                                 -Photos (optional) uses your own photo of each class as the card picture
#   add -NoServer to only do the database steps (e.g. in a second window while the site runs)
#   .\start_portal.ps1 -FakeMigration 0008 -NoServer
#                                 mark portal migration 0008 as applied WITHOUT running it, for a
#                                 change that is already in the Azure database

#   .\start_portal.ps1 -Manage "reconcile_schema" -NoServer
#                                 run any other manage.py command against Azure (in quotes)

param([switch]$Migrate, [switch]$SeedScanCards, [string]$Photos, [switch]$NoServer, [string]$FakeMigration,
      [string]$Manage)

Set-Location $PSScriptRoot
. .\venv\Scripts\Activate.ps1

$env:DB_ENGINE = "mssql"
$env:DB_HOST   = "plantdex-fyp15-ea.database.windows.net"
$env:DB_NAME   = "PlantDexDB"
$env:DB_DRIVER = "ODBC Driver 18 for SQL Server"
$env:DB_USER   = Read-Host "Azure admin login"
$env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR((Read-Host "Azure password" -AsSecureString)))

if ($Manage) {
    python manage.py $($Manage -split ' ')
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

if ($FakeMigration) {
    python manage.py migrate portal $FakeMigration --fake
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Could not mark migration $FakeMigration as applied (see the error above)." -ForegroundColor Yellow
        exit 1
    }
}

if ($Migrate) {
    python manage.py migrate
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Migrate failed. If it timed out, the database was asleep: wait 30 s and run again." -ForegroundColor Yellow
        exit 1
    }
}

if ($SeedScanCards) {
    # The first connection fails while a paused Azure database wakes up, so try a few times
    # (the seed skips cards that already exist, so repeating it is safe).
    foreach ($try in 1..4) {
        if ($Photos) { python manage.py seed_scan_collectibles --photos $Photos }
        else { python manage.py seed_scan_collectibles }
        if ($LASTEXITCODE -eq 0) { break }
        if ($try -eq 4) {
            Write-Host "Seeding failed (see the error above). 'Login failed' = wrong Azure login or password." -ForegroundColor Yellow
            break
        }
        Write-Host "Could not reach the database (it may be waking up), trying again in 20 s ($try/3)..." -ForegroundColor Yellow
        Start-Sleep -Seconds 20
    }
}

if (-not $NoServer) { python manage.py runserver }
