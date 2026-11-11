<#
  Instala el launcher en la TV Box por ADB y lo deja como inicio.
  Uso:  powershell -File scripts/instalar.ps1 -Ip <IP de tu TV> [-Apk out/launcher.apk]
  Requisitos: depuración por red activa en la TV Box y la conexión autorizada en su pantalla.
#>
param(
  [Parameter(Mandatory=$true)][string]$Ip,
  [string]$Apk = (Join-Path $PSScriptRoot "..\out\launcher.apk")
)
$ErrorActionPreference = "Stop"
$paquete = "cl.itatagroup.launcher"

$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) { $adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe" }
if (-not (Test-Path $adb)) { throw "No encuentro adb. Instala platform-tools o ponlo en el PATH." }
if (-not (Test-Path $Apk)) { throw "No existe $Apk. Corre antes: node tools/build.mjs" }

$destino = "${Ip}:5555"
& $adb connect $destino | Out-Null
$estado = (& $adb devices | Select-String $Ip) -join ""
if ($estado -match "unauthorized") { throw "La TV Box no autorizó la conexión: acepta el diálogo de depuración en su pantalla." }
if ($estado -notmatch "device") { throw "No hay conexión con $destino." }

& $adb -s $destino install -r $Apk
if ($LASTEXITCODE -ne 0) { throw "Falló la instalación (si dice firma distinta, desinstala primero $paquete)." }

# Fijar el nuestro como inicio no basta mientras otro inicio de mayor prioridad siga habilitado
# (en la TV Box, el launcher de Google). Como el nuestro ya está instalado, se desactivan los
# conocidos (reversible: pm enable --user 0 <paquete>). Si no están en el equipo, se ignora.
& $adb -s $destino shell cmd package set-home-activity "$paquete/.Home" | Out-Null
# Solo los que existen en el equipo: el Philips no tiene el de Xiaomi, y el error de adb por un
# paquete desconocido abortaría el script con "Stop" en PowerShell 5.1.
$instalados = (& $adb -s $destino shell pm list packages) -join "`n"
foreach ($otro in "com.google.android.tvlauncher", "com.mitv.tvhome.atv") {
  if ($instalados -match "package:$([regex]::Escape($otro))(\r?\n|$)") {
    & $adb -s $destino shell pm disable-user --user 0 $otro | Out-Host
  }
}
& $adb -s $destino shell input keyevent KEYCODE_HOME
Start-Sleep -Seconds 2

# Comprobar de verdad quién es el inicio ahora.
$inicio = (& $adb -s $destino shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME) -join "`n"
if ($inicio -notmatch [regex]::Escape($paquete)) {
  throw "Instalado, pero el inicio sigue siendo otro. Revisa con: adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME"
}
Write-Host "Instalado y fijado como inicio en $destino."
