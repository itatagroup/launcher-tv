# Actualizaciones del inicio

El inicio puede buscar una versión nueva de sí mismo, **solo cuando lo pides**: mantén OK sobre cualquier
app → «Buscar actualización del inicio» (la última opción del menú). Nunca consulta solo: ni al encender,
ni en segundo plano, ni con un temporizador. Es la única razón por la que pide `INTERNET` y
`REQUEST_INSTALL_PACKAGES`.

El mecanismo es el mismo contrato que usan el emisor de Windows y el receptor de `pantalla-tv` (el código
de Android es copia de `pantalla-tv/android-comun/java/cl/itatagroup/actualizacion/`: `Actualizador.java`,
`FirmaManifiesto.java` y `ResultadoInstalacion.java`). Como este repo tiene una sola app, el manifiesto es el
`version.json` del último release (`releases/latest`); en `pantalla-tv`, con tres apps, cada una tiene el suyo.

## Qué consulta y a dónde

Un GET sin cookies ni identificadores, con User-Agent `Inicio/<versión>` y nada del equipo ni del uso, a:

- `https://github.com/itatagroup/launcher-tv/releases/latest/download/version.json`
- `https://github.com/itatagroup/launcher-tv/releases/latest/download/version.json.sig`

GitHub redirige al release publicado más nuevo (los borradores no cuentan).

## Formato de `version.json`

UTF-8 sin BOM:

```json
{
  "formato": 1,
  "repo": "itatagroup/launcher-tv",
  "apps": {
    "launcher-android": {
      "version": "0.4.0",
      "versionCode": 4,
      "url": "https://github.com/itatagroup/launcher-tv/releases/download/v0.4.0/inicio-0.4.0.apk",
      "tamano": 46080,
      "sha256": "hex en minúsculas",
      "notas": "texto corto en español"
    }
  }
}
```

`version.json.sig` es la firma, en base64 y en una línea, de los bytes **exactos** de `version.json`:
ECDSA P-256 + SHA-256, en formato DER.

## Clave pública

Va dentro de la app (`Actualizador.CLAVE_PUBLICA`), como SubjectPublicKeyInfo DER en base64:

```
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAENdrCrDU1A0B87/zG/2dD80YPGfx8+gK2CclanFT9+8hPOlWFc9Q4zYXJqMEdQEd6bdsQ2eMgjqwhea0vcj95zA==
```

La privada está en `keystore/actualizaciones.pem` (PKCS#8 PEM) del PC que publica, ignorada por git. Solo
la usa `tools/firmar-manifiesto.mjs`, que además se niega a firmar si la pública que se deriva de ella no es
la de arriba. Si se pierde, hay que generar otra, cambiar la constante y publicar una versión nueva por
ADB: las TV con la versión vieja ya no aceptarían manifiestos nuevos.

## Qué comprueba la app, en orden

Si algo falla, no ofrece nada: muestra «No se pudo comprobar» (sin detalles técnicos; el motivo queda en
`adb logcat -s Actualizador`).

1. Solo HTTPS, también en cada redirección. `version.json` y `.sig` de 64 KB como máximo; 10 s de espera.
2. La firma, con la clave pública de la app.
3. `formato == 1`, `repo == "itatagroup/launcher-tv"` y la entrada `launcher-android`.
4. `versionCode` mayor que el instalado (si no: «El inicio está al día»).
5. `url` que empiece con `https://github.com/itatagroup/launcher-tv/releases/download/`.
6. Descarga a `cache/actualizacion/` de la app: se corta si pasa de `tamano`; al terminar, el tamaño tiene
   que ser exactamente `tamano` y el SHA-256 igual a `sha256`. Si no, se borra.
7. Instalación con `PackageInstaller`: **Android siempre muestra su confirmación**; nada es silencioso. La
   primera vez (Android 8+) hay que permitir «Instalar apps desconocidas» para Inicio: la app lo explica y
   abre ese ajuste. El archivo descargado se borra en cuanto se entrega a Android.

## Cómo publicar una versión

1. Sube `versionCode` y `versionName` en `version.json`, commitea y empuja (el script se niega con cambios sin
   commitear o con el commit sin empujar: el release se ancla a ese commit, para que quien compile desde el tag
   obtenga lo mismo que se publica).
2. Arma los archivos:

   ```powershell
   powershell -File scripts/publicar.ps1 -Version 0.4.0 -Notas "Qué cambia, en una frase."
   ```

   Compila, y deja en `dist/publicar/v0.4.0/` (ignorado por git): `inicio-0.4.0.apk`, `version.json`,
   `version.json.sig`, `SHA256SUMS.txt` y `notas.md`. Al final **imprime** el comando
   `gh release create v0.4.0 --target <commit> ... --draft`, que no corre solo.
3. Corre ese comando, revisa el borrador en GitHub y publícalo. Si la página pública de descargas enlaza la
   versión (la de Itatagroup, `itatagroup.cl/utilidades/limpiar-tv/`), actualiza ahí el enlace, la huella y el
   análisis de VirusTotal. Recién publicado pasa a ser el «latest»
   que encuentran las TV.

La APK tiene que ir firmada con la misma clave del almacén (`keystore/launcher.jks`) que la instalada: si
no, Android rechaza la actualización.

## Probar

- La verificación de firma, en el PC: `node tools/probar-firma.mjs` (necesita `javac` y `java`). Compila
  `FirmaManifiesto.java` tal cual y comprueba que una firma buena da `true` y que un byte cambiado, otra
  clave o una firma rota dan `false`.
- En una TV: con un release publicado de versión mayor a la instalada, «Buscar actualización del inicio»
  tiene que ofrecerla y, con «Actualizar», terminar en la confirmación de Android.
