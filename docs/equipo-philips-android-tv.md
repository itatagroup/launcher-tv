# TV Philips — qué se hizo y cómo revertirlo

Equipo: TV Philips FHD 32" (`PH0M_LT_T32_2K`, firmware TP Vision sobre MediaTek), Android 11,
1,3 GB de RAM, ADB por red en el puerto 5555 (en la IP de tu TV). Probado en noviembre de 2026, con el
mismo criterio que la Xiaomi (`equipo-xiaomi-mi-tv-stick.md`). Todo está automatizado en
`scripts/optimizar-philips.ps1` (limpieza y ajustes) y `scripts/instalar.ps1 -Ip <IP de tu TV>`
(launcher propio como inicio).

Nada se borró del sistema: lo «quitado» sale solo del usuario 0 y lo «desactivado» queda inactivo.

## Quitado para el usuario 0

Revertir: `adb shell pm install-existing --user 0 <paquete>`.

| Paquete | Qué era |
|---|---|
| `org.droidtv.gel` (GEL) | Telemetría de Philips: `GatheringService` |
| `org.droidtv.candeebug` (DAC) | Telemetría de Philips: `CollectionService` (~10 MB residente) |
| `org.droidtv.tv.frameworklogger` | Registro del framework de Philips |
| `org.droidtv.nettvregistration` | Registro Smart TV de Philips |
| `com.tpv.xmic.ots.cms.client` | Cliente CMS de TPV, versión `0.6.04_ad` (publicidad) |
| `android.autoinstalls.config.tpv.device` | Reinstalaba apps de fábrica por la Play Store |
| `com.google.android.partnersetup`, `.onetimeinitializer`, `.sss.authbridge` | Configuración inicial y de socio de Google |
| `com.google.android.backdrop` | Salvapantallas Ambient de Google (~78 MB) |
| `com.google.android.marvin.talkback`, `.syncadapters.calendar` | TalkBack y sincronización de calendario |
| `com.netflix.ninja`, `com.amazon.amazonvideo.livingroom`, `com.disney.disneyplus` | Netflix, Prime Video y Disney+ de fábrica (ya estaban desactivados) |
| `org.droidtv.nettv.market`, `org.droidtv.eum` | Galería de apps y manual electrónico de Philips |
| `com.android.htmlviewer`, `com.android.dreams.basic` | Visor HTML y salvapantallas básico |

## Desactivado

Revertir: `adb shell pm enable --user 0 <paquete>`.

| Paquete | Qué era | Nota |
|---|---|---|
| `com.google.android.tvlauncher` | Launcher de Google | Ya estaba desactivado; lo reafirma `instalar.ps1` |
| `com.google.android.tungsten.setupwraith`, `com.mediatek.wwtv.setupwizard` | Asistentes de configuración inicial | El equipo ya está configurado |
| `com.mediatek.android.leanbacklauncher.partnercustomizer` | Personalización del launcher de fábrica | |
| `com.android.tv.ota` | Actualizaciones OTA del firmware | **Reactivar para actualizar el firmware** |

## Otros launchers de terceros

Si tenías otro launcher de terceros (por ejemplo Projectivy, ~96 MB de RAM), puedes desinstalarlo: el
launcher propio lo reemplaza. Se reinstala desde la Play Store si quieres volver.

## Instalado

- `cl.itatagroup.launcher` (este repo), fijado como inicio; desde la 0.3.0: favoritas, banners,
  menú de mantener OK y salvapantallas.

Ocultos en el launcher (`res/values/config.xml`), siguen instalados: «ToU»
(`com.android.tv.tou`), «Disclaimer Customization» y «Televisión en directo»
(`com.mediatek.tv.service`, un atajo a Netflix; el servicio de TV sigue funcionando).
«Televisión en vivo» (`com.mediatek.wwtv.tvcenter`) **queda visible: son las entradas HDMI y la
antena**. «MultiMediaPlayer» (reproductor USB de MediaTek) también queda.

## Ajustes

Los mismos de la Xiaomi (`user_experience_enabled 0`, `upload_log_pref 0`,
`send_action_app_error 0`, `upload_apk_enable 0`, `limit_ad_tracking 1`,
`network_recommendations_enabled 0`, animaciones a 0.5), más:

| Ajuste | Valor | Para qué |
|---|---|---|
| `global activity_starts_logging_enabled` | 0 | Registro de cada app que se abre |
| `secure screensaver_components` | `cl.itatagroup.launcher/.ClockDream` | Salvapantallas propio del launcher (0.3.0): reloj que se desplaza, sin red; reemplaza a Backdrop |
| `secure screensaver_enabled` | 1 | Lo enciende tras el tiempo de espera del sistema (`screen_off_timeout`, 5 min) |

## Lo que se dejó a propósito

YouTube, Play Store, los servicios de Google (`gms`, `gsf`; los necesitan las dos anteriores),
el control por voz (`katniss` y `tts`, botón de micrófono) y Chromecast (`mediashell`), ambos a elección del dueño del equipo; el teclado (`inputmethod.latin`), el servicio de control remoto
de Google (`tv.remote.service`), WebView y todo el núcleo de TV de Philips/MediaTek (`org.droidtv`,
`org.droidtv.icp` —persistente—, `ambisvc`, `com.mediatek.*`, menús de fábrica). Y todas las apps
instaladas por el usuario (en el equipo de referencia: Gestor de archivos +, Xuper, Xuper Hydra, Downloader, BrowseHere y VLC).

## Resultado medido

RAM disponible (`MemAvailable`): ~609 MB antes → ~700–728 MB después del reinicio. El inicio pasó
de ~96 MB (Projectivy) a ~35–48 MB. Tras reiniciar, el launcher arranca como inicio y nada de lo
quitado volvió.

## Revertir todo

```bash
adb shell pm enable --user 0 com.google.android.tvlauncher
adb shell pm uninstall cl.itatagroup.launcher
```

y repetir `pm install-existing` / `pm enable` con el resto de las tablas (otro launcher, desde la
Play Store).
