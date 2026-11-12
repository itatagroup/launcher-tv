# Build, firma e instalación

## Requisitos

- Node 22+ y un JDK 17+ (sirve el que trae Android Studio).
- SDK de Android con `build-tools` y al menos una plataforma (`platforms/android-NN/android.jar`).
  Se detecta solo en `%LOCALAPPDATA%\Android\Sdk` (Windows), o con `ANDROID_HOME` / `ANDROID_SDK_ROOT`.
- Clave de firma en `keystore/launcher.jks` y su clave en `keystore/password.txt` (ambos fuera de git;
  ver «La clave de firma»).

## Compilar

```bash
node tools/build.mjs
```

Pasos: `aapt2 compile/link` (recursos, manifiesto, `R.java`) → `javac --release 8` → `d8` (dex) →
`jar uf` (mete el dex en el APK) → `zipalign` → `apksigner`. Resultado: `out/launcher.apk` (~17 KB).

La versión sale de `version.json`. **Sube `versionCode` en cada cambio que se vaya a publicar.**

## La clave de firma

Cada persona que compila usa su propia clave. Para crearla:

```bash
mkdir keystore
keytool -genkeypair -v -keystore keystore/launcher.jks -alias k -keyalg RSA -keysize 2048 -validity 9125
```

Guarda la clave que elegiste en `keystore/password.txt` (una sola línea) o pásala por la variable
`LAUNCHER_KEYSTORE_PASS`. Variables opcionales: `LAUNCHER_KEYSTORE` (ruta del `.jks`) y
`LAUNCHER_KEY_ALIAS` (por defecto `k`).

- `keystore/` está ignorado por git: **nunca subas la clave**.
- Android rechaza una actualización firmada con otra clave: si la cambias hay que desinstalar antes
  (`adb uninstall cl.itatagroup.launcher`). **Respalda el `.jks` y su clave fuera del PC.**

## Instalar en la TV

```bash
powershell -File scripts/instalar.ps1 -Ip <IP de tu TV>
```

Instala con `adb install -r`, lo fija como inicio, **desactiva el launcher de Google y los inicios de
fábrica que compiten con él** (si no, siguen ganando como inicio; se revierte con `pm enable`),
presiona Home y comprueba que el inicio quedó en `cl.itatagroup.launcher` (si no, falla). Con la TV
recién reiniciada hay que volver a autorizar ADB si cambió la clave del PC.

## Sin despliegue automático

Este repo **solo guarda el código**: no tiene workflows de CI ni publica nada por sí mismo. El APK se
compila en tu PC (`node tools/build.mjs`) y se instala en la TV con `scripts/instalar.ps1`, o se publica a
mano como release de GitHub con `scripts/publicar.ps1` para que las TV lo encuentren con «Buscar
actualización del inicio» (ver `docs/actualizaciones.md`).

## Verificación manual (no hay pruebas automáticas)

1. `node tools/build.mjs` termina sin errores.
2. `powershell -File scripts/instalar.ps1 -Ip <IP de tu TV>` instala y la TV muestra la cuadrícula.
3. `adb shell dumpsys activity activities | grep mResumedActivity` → `cl.itatagroup.launcher/.Home`.
4. `adb shell logcat -d -b crash` sin entradas del paquete.
5. Con el control remoto: moverse por la cuadrícula, abrir una app, volver con Home.
6. `adb shell dumpsys meminfo cl.itatagroup.launcher` → TOTAL PSS por debajo de ~40 MB (medido: ~20-36 MB).

Para comprobar una APK descargada, ver `docs/verificar.md`.
