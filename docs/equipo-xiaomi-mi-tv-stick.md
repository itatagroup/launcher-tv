# Xiaomi Mi TV (TV Box, Android 10) — qué se hace y cómo revertirlo

Equipo de referencia: Mi TV `MiTV-AESP0` (producto `aquaman`), Android 10, por ADB sobre la
red local (puerto 5555, en la IP de tu TV; la conexión se autoriza una vez en la pantalla de la TV). Probado en
noviembre de 2026. Todo lo de abajo está automatizado en `scripts/optimizar-tvbox.ps1` (limpieza y ajustes) y `scripts/instalar.ps1` (los dos inicios que compiten con el launcher propio).

Nada se borró del sistema: lo «quitado» sale solo del usuario 0 (el paquete sigue en la partición
del sistema) y lo «desactivado» queda inactivo.

## Quitado para el usuario 0

Revertir: `adb shell pm install-existing --user 0 <paquete>`.

| Paquete | Qué era |
|---|---|
| `com.miui.tv.analytics`, `com.xiaomi.statistic` | Telemetría de Xiaomi |
| `com.google.android.feedback`, `com.google.android.tv.bugreportsender` | Informes de errores |
| `com.google.android.music`, `.videos`, `.play.games`, `.youtube.tvmusic` | Play Música, Películas, Juegos y YouTube Music |
| `com.google.android.marvin.talkback` | TalkBack (accesibilidad) |
| `com.android.gallery3d`, `.htmlviewer`, `.printspooler`, `.dreams.basic` | Galería, visor HTML, impresión, salvapantallas |
| `com.google.android.syncadapters.contacts`, `.calendar` | Sincronización de contactos y calendario |
| `com.mitv.tvhome.mitvplus` | Mi TV+ (publicidad de Xiaomi) |

## Desactivado

Revertir: `adb shell pm enable --user 0 <paquete>`.

| Paquete | Qué era | Nota |
|---|---|---|
| `com.google.android.tvlauncher` | Launcher de Google (~81 MB de RAM) | Lo desactiva `scripts/instalar.ps1` tras instalar el launcher propio |
| `com.google.android.tvrecommendations` | Recomendaciones del launcher | Solo servían al launcher anterior |
| `com.mitv.tvhome.atv` | PatchWall, el inicio de Xiaomi | Es opcional; también lo desactiva `instalar.ps1` |
| `com.mitv.tvhome.michannel` | Canales de Xiaomi | Ídem |
| `com.xiaomi.mitv.updateservice` | Actualizaciones OTA del firmware | **Reactivar para actualizar el firmware** |
| `com.xiaomi.android.tvsetup.partnercustomizer`, `com.google.android.tungsten.setupwraith` | Asistentes de configuración inicial | El equipo ya está configurado |

## Apps de terceros

Si tenías otro launcher de terceros u otras apps que no usas, puedes desinstalarlas
(`adb shell pm uninstall <paquete>`); `optimizar-tvbox.ps1` trae para eso la lista `$desinstalar`, vacía por defecto.

## Ajustes

> Desde la 0.3.0 del launcher, `optimizar-tvbox.ps1` también fija el salvapantallas propio
> (`screensaver_components cl.itatagroup.launcher/.ClockDream`, `screensaver_enabled 1`). Requiere
> tener instalada la 0.3.0 o posterior. Antes de cambiarlo, el de este equipo era
> `com.google.android.backdrop/.Backdrop` (modo ambiente de Google); para volver a él:
> `adb shell settings put secure screensaver_components com.google.android.backdrop/.Backdrop`.

| Ajuste | Valor | Para qué |
|---|---|---|
| `global user_experience_enabled` | 0 | Programa de experiencia de usuario (envía datos a Xiaomi) |
| `secure upload_log_pref` | 0 | Subida de registros |
| `global send_action_app_error` | 0 | Informes de errores de apps |
| `global upload_apk_enable` | 0 | Subida de APK |
| `secure limit_ad_tracking` | 1 | Excluirse del seguimiento publicitario |
| `global network_recommendations_enabled` | 0 | Recomendaciones de redes |
| `window_animation_scale`, `transition_animation_scale`, `animator_duration_scale` | 0.5 | Interfaz más ágil |

No se encontró ningún ajuste que suba «fotos» o capturas al fabricante; los de arriba son los que
envían datos. Se corrió además `pm trim-caches 999G` y `cmd package bg-dexopt-job` (ambos están en `optimizar-tvbox.ps1`).

## Lo que se dejó a propósito

YouTube, Play Store, Netflix, Prime Video, los servicios de Google (`gms`, `gsf`), el control por
voz (`katniss`, lo usa el botón de micrófono), Chromecast (`mediashell`), `xiaomi.tvservice` y todas
las apps instaladas por el usuario (navegadores, gestor de archivos, Twitch, Send Files To TV, Xuper
Hydra).

## Resultado medido

RAM libre: ~290 MB al empezar → ~460 MB al terminar. El proceso del launcher pasó de ~81 MB a
~20–36 MB. No se midió tiempo de arranque de apps.

## Revertir todo

```bash
adb shell pm enable --user 0 com.google.android.tvlauncher
adb shell pm enable --user 0 com.mitv.tvhome.atv
adb shell pm uninstall cl.itatagroup.launcher
```

y repetir `pm install-existing` / `pm enable` con el resto de las tablas.
