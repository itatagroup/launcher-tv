# Verificar una APK descargada

Antes de instalar una APK de un release, comprueba que es la oficial. Hay cuatro formas, de menor a
mayor esfuerzo.

## 1. Suma de verificación (SHA-256)

Cada release publica `SHA256SUMS.txt`. Compara la suma de tu archivo con la de ese archivo.

PowerShell:

```powershell
Get-FileHash .\launcher.apk -Algorithm SHA256
```

Linux o macOS:

```bash
sha256sum launcher.apk
```

Los valores deben ser idénticos a la línea de `launcher.apk` en `SHA256SUMS.txt`.

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

El launcher no debe pedir internet. Mira el manifiesto con `aapt2` (en `build-tools`):

```bash
aapt2 dump permissions launcher.apk
```

Debe aparecer solo `android.permission.REQUEST_DELETE_PACKAGES` (para «Desinstalar»). Si aparece
`android.permission.INTERNET` u otro permiso, no es la APK oficial.
