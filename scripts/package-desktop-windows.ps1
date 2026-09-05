#Requires -Version 5.1
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $Root "desktop"))) {
  $Root = Get-Location
}

Set-Location $Root
$Resources = Join-Path $Root "desktop\src-tauri\resources"
New-Item -ItemType Directory -Force -Path $Resources | Out-Null

Write-Host "Building frontend SPA..."
Set-Location (Join-Path $Root "frontend")
npm ci
$env:VITE_API_URL = "/api"
npm run build

Write-Host "Packaging Spring Boot JAR with SPA..."
Set-Location (Join-Path $Root "backend")
mvn -B -Pdesktop -DskipTests package
$Jar = Get-ChildItem "target\inventory-management-system-*.jar" | Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*original*" } | Select-Object -First 1
if (-not $Jar) {
  throw "Desktop JAR was not built"
}
Copy-Item $Jar.FullName (Join-Path $Resources "app.jar") -Force

Write-Host "Linking a compact JRE..."
$JreDir = Join-Path $Resources "jre"
if (Test-Path $JreDir) {
  Remove-Item -Recurse -Force $JreDir
}
$Jlink = Join-Path $env:JAVA_HOME "bin\jlink.exe"
if (-not (Test-Path $Jlink)) {
  $Jlink = "jlink"
}
& $Jlink `
  --add-modules "java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.se,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.management,jdk.naming.dns,jdk.unsupported,jdk.xml.dom,jdk.zipfs" `
  --no-header-files `
  --no-man-pages `
  --strip-debug `
  --output $JreDir
if ($LASTEXITCODE -ne 0) {
  throw "jlink failed"
}

Write-Host "Collecting PostgreSQL binaries..."
$PgDir = Join-Path $Resources "pgsql"
if (Test-Path $PgDir) {
  Remove-Item -Recurse -Force $PgDir
}
$PgZip = Join-Path $env:TEMP "postgresql-windows-binaries.zip"
$PgUrl = "https://get.enterprisedb.com/postgresql/postgresql-16.6-1-windows-x64-binaries.zip"
try {
  Invoke-WebRequest -Uri $PgUrl -OutFile $PgZip -UseBasicParsing
  Expand-Archive -Path $PgZip -DestinationPath (Join-Path $env:TEMP "pgsql-extract") -Force
  $Extracted = Get-ChildItem (Join-Path $env:TEMP "pgsql-extract") -Directory | Select-Object -First 1
  if (Test-Path (Join-Path $env:TEMP "pgsql-extract\pgsql")) {
    Copy-Item (Join-Path $env:TEMP "pgsql-extract\pgsql") $PgDir -Recurse
  } elseif ($Extracted) {
    Copy-Item $Extracted.FullName $PgDir -Recurse
  } else {
    throw "Unexpected PostgreSQL zip layout"
  }
} catch {
  Write-Host "EDB zip failed ($($_.Exception.Message)); installing PostgreSQL with Chocolatey..."
  choco install postgresql16 --no-progress -y --params "/Password:InvLocal_app1"
  $ChocoPg = "C:\Program Files\PostgreSQL\16"
  New-Item -ItemType Directory -Force -Path $PgDir | Out-Null
  Copy-Item (Join-Path $ChocoPg "bin") (Join-Path $PgDir "bin") -Recurse
  Copy-Item (Join-Path $ChocoPg "lib") (Join-Path $PgDir "lib") -Recurse
  Copy-Item (Join-Path $ChocoPg "share") (Join-Path $PgDir "share") -Recurse
}

Write-Host "Building NSIS installer..."
Set-Location (Join-Path $Root "desktop")
if (-not (Test-Path "node_modules")) {
  npm ci
} else {
  npm ci
}
npx tauri build --bundles nsis
Write-Host "Installer output is under desktop\src-tauri\target\release\bundle\nsis\"
