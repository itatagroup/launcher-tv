# launcher-tv

Inicio (launcher) minimalista para Android TV. Muestra la hora y la fecha arriba, una fila de
**favoritas** grandes y, debajo, **todas las apps** con su banner de TV y su nombre. **Mantener OK**
sobre una app abre un menú para marcarla o quitarla de favoritas, ver su información, desinstalarla o
buscar una actualización del propio inicio.
Trae además un **salvapantallas** propio: un reloj que se desplaza por la pantalla.

Sin tienda, sin recomendaciones, sin publicidad y con un consumo de RAM mínimo (~35 MB en una TV
Philips, frente a ~81 MB del launcher de Google y ~96 MB de Projectivy).

- Paquete: `cl.itatagroup.launcher` · Android 5+ (minSdk 21) · sin dependencias (solo el SDK de Android).
- Licencia: GPL-3.0 (ver `LICENSE`).
- Rama de trabajo `dev`, estable `main`.

## Qué NO hace

- **Sin telemetría, sin cuentas y sin publicidad.**
- **No se conecta a internet si no se lo pides.** La única conexión posible es la búsqueda manual de
  actualización del propio inicio (ver «Actualizaciones» abajo): nunca consulta sola, ni al encender ni
  en segundo plano.
- Los únicos permisos que pide:
  - `REQUEST_DELETE_PACKAGES`: para la opción «Desinstalar» del menú de mantener OK (abre el diálogo
    de confirmación del sistema; nunca desinstala nada sin que lo confirmes).
  - `queries` (visibilidad de paquetes, Android 11+): para poder listar las apps instaladas; sin eso la
    lista quedaría vacía.
  - `INTERNET` y `REQUEST_INSTALL_PACKAGES`: **solo** para el botón «Buscar actualización».
  - `WRITE_SECURE_SETTINGS` y `RECEIVE_BOOT_COMPLETED`: para mantener apagada la telemetría (ver abajo).
    Android **no** concede `WRITE_SECURE_SETTINGS` al instalar: solo se da por ADB, y sin él no hace nada.

  Se pueden comprobar en `app/src/main/AndroidManifest.xml` y con `docs/verificar.md`.

## Mantener apagada la telemetría

Algunas teles vuelven a encender solas los ajustes de envío de datos: la Xiaomi Mi TV Stick pone otra vez en 1
`user_experience_enabled` y `upload_log_pref` en cada arranque (comprobado el 12/11/2026). El inicio los revisa
**al encender la tele y cada vez que vuelves a él**, y vuelve a apagar los que se encendieron. Solo toca la
lista de ajustes de `GuardianPrivacidad.java` (los del grupo «telemetría» de `scripts/catalogo-tv.json`); no lee
ni manda nada más, y no puede volver a desactivar apps (Android solo lo permite a las del sistema).

- Necesita un permiso que solo se da desde un PC: `scripts/limpiar-tv.ps1` lo da al instalar el inicio o al
  limpiar la telemetría, o a mano: `adb shell pm grant cl.itatagroup.launcher android.permission.WRITE_SECURE_SETTINGS`
  (se quita con `pm revoke`).
- El botón **«Telemetría»**, junto a la hora, dice si está vigilando, cuántas veces tuvo que volver a apagarla y
  cuándo fue la última, y permite apagarlo.

## Actualizaciones

El inicio **nunca consulta solo**. Solo si pulsas **«Buscar actualización»**, el botón que está junto a la
hora (se llega con la flecha arriba):

1. Pide, con un GET sin cookies ni datos del equipo (User-Agent `Inicio/<versión>`), dos archivos del
   último release de este repositorio en GitHub:
   `https://github.com/itatagroup/launcher-tv/releases/latest/download/version.json` y su firma `version.json.sig`.
2. Comprueba la firma con la clave pública que viene dentro de la app. Si no es válida, o si falla la
   red, avisa «No se pudo comprobar» y no hace nada más.
3. Si hay una versión mayor, te muestra la versión y sus notas con «Actualizar» / «Ahora no»; si no,
   «El inicio está al día».
4. Con «Actualizar» descarga la APK del release (solo HTTPS), comprueba su tamaño y su SHA-256 contra el
   manifiesto firmado y se la entrega a Android, que **siempre pide confirmar** la instalación. La primera
   vez Android pide además permitir «Instalar apps desconocidas» para Inicio; el inicio lo explica y abre
   ese ajuste.

No hay nada que apagar: si no eliges la opción, no hay conexión. Detalle del formato, la clave pública y
cómo publicar una versión en `docs/actualizaciones.md`.

## Requisitos

- Node 22+ y un JDK 17+ (sirve el que trae Android Studio).
- SDK de Android con `build-tools` y al menos una plataforma (`platforms/android-NN/android.jar`).
  Se detecta solo en `%LOCALAPPDATA%\Android\Sdk` (Windows) o con `ANDROID_HOME` / `ANDROID_SDK_ROOT`.
- Una clave de firma propia (ver abajo).
- Para instalar: ADB y la depuración por red activa en la TV, con la conexión autorizada en su pantalla.

## Compilar

```bash
node tools/build.mjs        # genera out/launcher.apk
```

La firma la espera así `tools/build.mjs`:

- El almacén de claves en `keystore/launcher.jks` (o la ruta de `LAUNCHER_KEYSTORE`).
- Su clave en `keystore/password.txt` o en la variable `LAUNCHER_KEYSTORE_PASS`.
- El alias de la clave es `k`, o el de `LAUNCHER_KEY_ALIAS`.

Para crear tu propia clave (la carpeta `keystore/` está ignorada por git; **nunca la subas**):

```bash
mkdir keystore
keytool -genkeypair -v -keystore keystore/launcher.jks -alias k -keyalg RSA -keysize 2048 -validity 9125
# escribe la clave que elegiste en keystore/password.txt (una sola línea)
```

La clave de firma es lo que permite que Android acepte actualizaciones encima de lo instalado: si la
pierdes o la cambias, hay que desinstalar el launcher antes de instalar el nuevo. Respáldala.
Más detalle en `docs/build-y-despliegue.md`.

## Instalar

```bash
powershell -File scripts/instalar.ps1 -Ip <IP de tu TV>
```

Instala con `adb install -r`, lo fija como inicio, desactiva los inicios de fábrica que compiten con
él y comprueba que el inicio quedó en `cl.itatagroup.launcher`. Todo es reversible (`pm enable`).

## Limpiar la tele

`scripts/limpiar-tv.ps1` limpia cualquier tele con Android TV o Google TV por ADB, de forma interactiva:
detecta la marca, cruza lo instalado con el catálogo `scripts/catalogo-tv.json` (telemetría, publicidad, apps de
fábrica, ajustes de envío de datos), muestra lo que haría y solo aplica lo que elijas. Puede instalar también este
inicio y Pantalla desde sus releases firmados. Nada se borra del sistema y deja un `revertir.ps1` con todo lo hecho.
Xiaomi y Philips tienen listas probadas; las demás marcas reciben solo lo genérico y seguro.

```bash
powershell -ExecutionPolicy Bypass -File scripts/limpiar-tv.ps1
```

Guía completa (activar la depuración, cada grupo, deshacer, comandos a mano, sumar una marca):
`docs/limpiar-tv.md`. Para hacerlo con un agente de IA: `docs/limpiar-con-agente.md`. **Bajo tu
responsabilidad**: revisa la lista antes de aplicar.

## Estructura

```
app/src/main/
  AndroidManifest.xml          HOME + LEANBACK_LAUNCHER
  java/cl/itatagroup/launcher/
    Home.java                  inicio: botones junto a la hora, favoritas + todas, menú de mantener OK, Atrás no sale
    GuardianPrivacidad.java    mantiene apagados los ajustes de envío de datos (con permiso dado por ADB)
    AlEncender.java            al arrancar la tele, le pide al guardián que revise
    Actualizador.java          búsqueda manual de actualización: manifiesto firmado, descarga e instalación
    FirmaManifiesto.java       verificación de la firma del manifiesto (Java puro, se prueba en el PC)
    ResultadoInstalacion.java  resultado de la instalación: abre la confirmación de Android
    Favorites.java             favoritas en orden, guardadas en el equipo (la lista de partida en config.xml)
    ClockDream.java            salvapantallas: hora y fecha que cambian de lugar cada minuto
    AppCatalog.java            arma la lista: apps de TV y normales, sin repetir, sin ocultas
    AppsAdapter.java           vista de cada app (icono + nombre)
    AppEntry.java              una app lanzable
  res/values/config.xml        columnas, favoritas de partida y paquetes ocultos (lo más fácil de cambiar)
  res/values/{colors,dimens,strings}.xml, res/layout/   aspecto
tools/build.mjs                build y firma, igual en Windows y en Linux/macOS
tools/firmar-manifiesto.mjs    firma version.json para un release (con keystore/actualizaciones.pem)
tools/probar-firma.mjs         prueba en el PC la verificación de firma del actualizador
scripts/publicar.ps1           arma dist/publicar/v<versión>/ para un release de GitHub
scripts/instalar.ps1           instala por ADB una APK compilada aquí y fija el inicio
scripts/limpiar-tv.ps1         limpieza interactiva y reversible de una tele (ver docs/limpiar-tv.md)
scripts/catalogo-tv.json       qué paquetes y ajustes toca la limpieza, por grupo y marca
docs/                          documentos de equipo, build y firma, verificación de una APK
version.json                   versionCode / versionName
```

## Documentos

- `docs/build-y-despliegue.md` — compilar, firmar e instalar.
- `docs/verificar.md` — cómo comprobar que una APK descargada es la oficial.
- `docs/actualizaciones.md` — la búsqueda de actualización: contrato, clave pública y cómo publicar.
- `docs/equipo-xiaomi-mi-tv-stick.md` y `docs/equipo-philips-android-tv.md` — qué se quita y ajusta en cada equipo, y cómo revertirlo.
- `CONTRIBUTING.md`, `SECURITY.md` y `AGENTS.md`.

## Licencia

GPL-3.0. Ver `LICENSE`.
