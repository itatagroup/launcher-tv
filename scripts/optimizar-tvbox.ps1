<#
  Aplica a la TV Box (Mi TV, Android 10) la limpieza y los ajustes documentados en
  docs/equipo-xiaomi-mi-tv-stick.md. Es idempotente y reversible: no borra nada del sistema, solo lo quita
  para el usuario 0 o lo desactiva. No toca las apps instaladas por el usuario.
  Uso:  powershell -File scripts/optimizar-tvbox.ps1 -Ip <IP de tu TV> [-SoloVer]
#>
param(
  [Parameter(Mandatory=$true)][string]$Ip,
  [switch]$SoloVer
)
$ErrorActionPreference = "Stop"

# Quitar para el usuario 0 (se restauran con: pm install-existing --user 0 <paquete>)
$quitar = @(
  "com.miui.tv.analytics", "com.xiaomi.statistic",                       # telemetría
  "com.google.android.feedback", "com.google.android.tv.bugreportsender", # informes
  "com.google.android.music", "com.google.android.videos",
  "com.google.android.play.games", "com.google.android.youtube.tvmusic",
  "com.google.android.marvin.talkback", "com.android.gallery3d",
  "com.android.htmlviewer", "com.android.printspooler", "com.android.dreams.basic",
  "com.google.android.syncadapters.contacts", "com.google.android.syncadapters.calendar",
  "com.mitv.tvhome.mitvplus"
)
# Desactivar (se restauran con: pm enable --user 0 <paquete>)
# Los inicios de Google (tvlauncher) y de Xiaomi (PatchWall) NO se tocan aquí: los desactiva
# scripts/instalar.ps1 justo después de instalar el launcher propio, para no dejar nunca la TV sin inicio.
$desactivar = @(
  "com.google.android.tvrecommendations",                                  # recomendaciones del launcher de Google
  "com.mitv.tvhome.michannel",                                             # canales de Xiaomi
  "com.xiaomi.mitv.updateservice",                                         # actualizaciones OTA (reactivar para actualizar el firmware)
  "com.xiaomi.android.tvsetup.partnercustomizer", "com.google.android.tungsten.setupwraith"
)
# Apps instaladas por el usuario que quiera desinstalar del todo (vacío por defecto)
$desinstalar = @()
# Ajustes de envío de datos y rendimiento
$ajustes = @(
  "settings put global user_experience_enabled 0",
  "settings put secure upload_log_pref 0",
  "settings put global send_action_app_error 0",
  "settings put global upload_apk_enable 0",
  "settings put secure limit_ad_tracking 1",
  "settings put global network_recommendations_enabled 0",
  # Salvapantallas: el reloj propio del launcher (ClockDream, desde la 0.3.0).
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
foreach ($p in $desinstalar) { Ejecutar "desinstalar $p" @("pm", "uninstall", $p) }
Ejecutar "limpiar caché" @("pm", "trim-caches", "999G")
# Recompila en segundo plano; puede tardar varios minutos (el comando espera a que termine).
Ejecutar "optimizar apps (dexopt)" @("cmd", "package", "bg-dexopt-job")
