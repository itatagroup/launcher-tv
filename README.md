# launcher-tv

Inicio (launcher) minimalista para Android TV. Muestra la hora y la fecha arriba, una fila de
**favoritas** grandes y, debajo, **todas las apps** con su banner de TV y su nombre. **Mantener OK**
sobre una app abre un menú para marcarla o quitarla de favoritas, ver su información o desinstalarla.
Trae además un **salvapantallas** propio: un reloj que se desplaza por la pantalla.

Sin tienda, sin recomendaciones, sin publicidad y con un consumo de RAM mínimo (~35 MB en una TV
Philips, frente a ~81 MB del launcher de Google y ~96 MB de Projectivy).

- Paquete: `cl.itatagroup.launcher` · Android 5+ (minSdk 21) · sin dependencias (solo el SDK de Android).
- Licencia: GPL-3.0 (ver `LICENSE`).
- Rama de trabajo `dev`, estable `main`.

## Qué NO hace

- **Sin telemetría, sin cuentas y sin publicidad.**
- **Sin internet**: el launcher no pide el permiso `INTERNET` (se puede comprobar en
  `app/src/main/AndroidManifest.xml` y con `docs/verificar.md`), así que no puede conectarse a ninguna red.
  Está planeada una **búsqueda de actualizaciones opcional y que se puede apagar**: cuando llegue,
  esta sección dirá exactamente qué consulta, a dónde, y cómo desactivarla.
- Los únicos permisos que pide:
  - `REQUEST_DELETE_PACKAGES`: para la opción «Desinstalar» del menú de mantener OK (abre el diálogo
    de confirmación del sistema; nunca desinstala nada sin que lo confirmes).
  - `queries` (visibilidad de paquetes, Android 11+): para poder listar las apps instaladas; sin eso la
    lista quedaría vacía.

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

## Limpieza opcional del equipo

Estos scripts quitan (solo para el usuario 0) o desactivan apps de telemetría y publicidad del
fabricante y ajustan envío de datos y animaciones. Nada se borra del sistema y cada cambio se puede
revertir; el detalle de cada paquete y cómo volver atrás está en los documentos de equipo. Son
opcionales y **se usan bajo tu responsabilidad**: revisa la lista antes de correrlos.

- `scripts/optimizar-tvbox.ps1 -Ip <IP de tu TV>` — Xiaomi Mi TV: `docs/equipo-xiaomi-mi-tv-stick.md`.
- `scripts/optimizar-philips.ps1 -Ip <IP de tu TV>` — Philips Android TV: `docs/equipo-philips-android-tv.md`.

Ambos aceptan `-SoloVer` para mostrar lo que harían sin aplicar nada.

## Estructura

```
app/src/main/
  AndroidManifest.xml          HOME + LEANBACK_LAUNCHER
  java/cl/itatagroup/launcher/
    Home.java                  inicio: favoritas + todas, lanzar app, menú de mantener OK, Atrás no sale
    Favorites.java             favoritas en orden, guardadas en el equipo (la lista de partida en config.xml)
    ClockDream.java            salvapantallas: hora y fecha que cambian de lugar cada minuto
    AppCatalog.java            arma la lista: apps de TV y normales, sin repetir, sin ocultas
    AppsAdapter.java           vista de cada app (icono + nombre)
    AppEntry.java              una app lanzable
  res/values/config.xml        columnas, favoritas de partida y paquetes ocultos (lo más fácil de cambiar)
  res/values/{colors,dimens,strings}.xml, res/layout/   aspecto
tools/build.mjs                build y firma, igual en Windows y en Linux/macOS
scripts/instalar.ps1           instala por ADB y fija el inicio
scripts/optimizar-tvbox.ps1    limpieza y ajustes de la Xiaomi (reversible)
scripts/optimizar-philips.ps1  limpieza y ajustes de la Philips (reversible)
docs/                          documentos de equipo, build y firma, verificación de una APK
version.json                   versionCode / versionName
```

## Documentos

- `docs/build-y-despliegue.md` — compilar, firmar e instalar.
- `docs/verificar.md` — cómo comprobar que una APK descargada es la oficial.
- `docs/equipo-xiaomi-mi-tv-stick.md` y `docs/equipo-philips-android-tv.md` — qué se quita y ajusta en cada equipo, y cómo revertirlo.
- `CONTRIBUTING.md`, `SECURITY.md` y `AGENTS.md`.

## Licencia

GPL-3.0. Ver `LICENSE`.
