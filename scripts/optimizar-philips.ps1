<#
  Aplica a la TV Philips (PH0M, Android 11, MediaTek) la limpieza y los ajustes documentados en
  docs/equipo-philips-android-tv.md. Es idempotente y reversible: no borra nada del sistema, solo lo quita para
  el usuario 0 o lo desactiva. No toca las apps instaladas por el usuario.
  Uso:  powershell -File scripts/optimizar-philips.ps1 -Ip <IP de tu TV> [-SoloVer]
  Después: scripts/instalar.ps1 -Ip <IP de tu TV> (deja el launcher propio como inicio).
#>
param(
  [Parameter(Mandatory=$true)][string]$Ip,
  [switch]$SoloVer
)
$ErrorActionPreference = "Stop"

# Quitar para el usuario 0 (se restauran con: pm install-existing --user 0 <paquete>)
$quitar = @(
  "org.droidtv.gel",                         # GEL: «GatheringService», recolección de datos de Philips
  "org.droidtv.candeebug",                   # DAC: «CollectionService», recolección de datos
  "org.droidtv.tv.frameworklogger",          # registro del framework de Philips
  "org.droidtv.nettvregistration",           # registro Smart TV de Philips
  "com.tpv.xmic.ots.cms.client",             # cliente CMS de TPV (versión «_ad»: publicidad)
  "android.autoinstalls.config.tpv.device",  # reinstala apps de fábrica por la Play Store
  "com.google.android.partnersetup", "com.google.android.onetimeinitializer",
  "com.google.android.sss.authbridge",
  "com.google.android.backdrop",             # salvapantallas Ambient de Google (~78 MB)
  "com.google.android.marvin.talkback", "com.google.android.syncadapters.calendar",
  "com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus",
  "org.droidtv.nettv.market",                # galería de apps de Philips
  "org.droidtv.eum",                         # manual electrónico
  "com.android.htmlviewer", "com.android.dreams.basic"
)
# Desactivar (se restauran con: pm enable --user 0 <paquete>)
# El launcher de Google (tvlauncher) lo desactiva scripts/instalar.ps1 tras instalar el propio.
$desactivar = @(
  "com.google.android.tungsten.setupwraith", "com.mediatek.wwtv.setupwizard",   # asistentes de configuración
  "com.mediatek.android.leanbacklauncher.partnercustomizer",
  "com.android.tv.ota"                                                          # actualizaciones OTA (reactivar para actualizar el firmware)
)
# Ajustes de envío de datos, salvapantallas y rendimiento
$ajustes = @(
  "settings put global user_experience_enabled 0",
  "settings put secure upload_log_pref 0",
  "settings put global send_action_app_error 0",
  "settings put global upload_apk_enable 0",
  "settings put secure limit_ad_tracking 1",
  "settings put global network_recommendations_enabled 0",
  "settings put global activity_starts_logging_enabled 0",
  # Salvapantallas: el reloj propio del launcher (ClockDream), no el de Google.
  "settings put secure screensaver_components cl.itatagroup.launcher/.ClockDream",
  "settings put secure screensaver_enabled 1",
  "settings put global window_animation_scale 0.5",
  "settings put global transition_animation_scale 0.5",
  "settings put global animator_duration_scale 0.5"
)

$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) { $adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe" }
if (-not (Test-Path $adb)) { throw "No encuentro adb." }
$d = "${Ip}:5555"
& $adb connect $d | Out-Null
if (((& $adb devices | Select-String $Ip) -join "") -notmatch "\sdevice") { throw "Sin conexión autorizada con $d." }

function Ejecutar($texto, $cmd) {
  if ($SoloVer) { Write-Host "[simulado] $texto"; return }
  # En PowerShell 5.1 el stderr de un ejecutable nativo, con "Stop", abortaría todo el script.
  $previo = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try { $r = & $adb -s $d shell @cmd 2>&1 } finally { $ErrorActionPreference = $previo }
  Write-Host "$texto -> $($r -join ' ')"
}

foreach ($p in $quitar)     { Ejecutar "quitar $p"     @("pm", "uninstall", "--user", "0", $p) }
foreach ($p in $desactivar) { Ejecutar "desactivar $p" @("pm", "disable-user", "--user", "0", $p) }
foreach ($a in $ajustes)    { Ejecutar $a              ($a -split " ") }
Ejecutar "limpiar caché" @("pm", "trim-caches", "999G")
