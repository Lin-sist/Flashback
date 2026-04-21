param(
    [string]$DbUsername = "root",
    [string]$DbPassword = "123456",
    [string]$Profile = "dev"
)

$env:DB_USERNAME = $DbUsername
$env:DB_PASSWORD = $DbPassword

Write-Host "Starting backend with profile '$Profile' and DB user '$DbUsername'..."
Write-Host "DB_PASSWORD has been set for this process."

mvn spring-boot:run "-Dspring-boot.run.profiles=$Profile"
