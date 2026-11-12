# Verificar una APK descargada

Antes de instalar una APK de un release, comprueba que es la oficial. Hay cuatro formas, de menor a
mayor esfuerzo.

## 1. Suma de verificación (SHA-256)

Cada release publica `SHA256SUMS.txt`. Compara la suma de tu archivo con la de ese archivo.

PowerShell:

```powershell
Get-FileHash .\inicio-0.4.0.apk -Algorithm SHA256
```

Linux o macOS:

```bash
sha256sum inicio-0.4.0.apk
```

Los valores deben ser idénticos a la línea de la APK en `SHA256SUMS.txt` (desde la 0.4.0 la APK del release
se llama `inicio-<versión>.apk`; también trae `version.json` y su firma `version.json.sig`, ver
`docs/actualizaciones.md`).

## 2. Certificado de firma

Con `apksigner` (viene en `build-tools` del SDK de Android):

```bash
apksigner verify --print-certs launcher.apk
```

La línea `certificate SHA-256 digest` debe coincidir con la huella publicada del proyecto:

`<huella SHA-256 del certificado: se publica con el primer release>`

Si la huella es distinta, la APK no la firmó el mantenedor: no la instales.

## 3. Compilarla tú mismo y comparar

```bash
git clone <URL del repositorio> && cd launcher-tv
git checkout <etiqueta del release>
node tools/build.mjs        # con tu propia clave de firma, ver README.md
```

Descomprime ambas APK (una APK es un zip) y compara todo el contenido **salvo `META-INF/`**, que es
donde va la firma y por eso difiere con una clave distinta:

```bash
mkdir a b
(cd a && unzip -q ../launcher-descargada.apk)
(cd b && unzip -q ../out/launcher.apk)
diff -r -x META-INF a b
```

Sin diferencias significa que la APK descargada corresponde al código fuente. Puede haber diferencias
menores si usas otra versión de las build-tools o del JDK; compara entonces `classes.dex` y los
recursos con las mismas herramientas.

## 4. Revisar los permisos

Mira el manifiesto con `aapt2` (en `build-tools`):

```bash
aapt2 dump permissions launcher.apk
```

Desde la 0.4.0 deben aparecer exactamente estos tres:

- `android.permission.REQUEST_DELETE_PACKAGES` (para «Desinstalar»).
- `android.permission.INTERNET` y `android.permission.REQUEST_INSTALL_PACKAGES` (solo para «Buscar
  actualización del inicio», ver `docs/actualizaciones.md`).

Si aparece otro permiso, no es la APK oficial.
