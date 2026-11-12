# Limpiar una tele Android

Cómo sacarle a una tele con Android TV o Google TV la telemetría, la publicidad y las apps de fábrica que no usas,
dejarle un inicio sin publicidad y, si quieres, la app para ver el PC o el teléfono en ella. Todo por ADB desde un
PC, **sin root**, de forma **reversible** y **determinista**: el mismo catálogo da el mismo resultado en el mismo
equipo, sin depender de nadie que «decida» en el momento.

Hay tres formas, de la más simple a la más manual:

1. **El script interactivo** `scripts/limpiar-tv.ps1` (recomendado).
2. **Los comandos ADB a mano**, al final de esta guía.
3. **Con un agente de IA**, si prefieres que te guíe uno: `docs/limpiar-con-agente.md`.

> **Bajo tu responsabilidad.** Nada se borra del sistema y todo se puede revertir, pero es tu equipo. Lee la lista
> antes de aplicar. Lo que el script no reconoce, no lo toca.

## Antes de empezar

1. **ADB en el PC**: instala las *Android SDK Platform-Tools* (descarga oficial de Google) y deja `adb` en el PATH.
   En Windows también sirve la de Android Studio (`%LOCALAPPDATA%\Android\Sdk\platform-tools`), que el script
   encuentra sola.
2. **PowerShell**: en Windows ya viene. En Mac o Linux instala PowerShell 7 (`pwsh`).
3. **Depuración en la tele**: Ajustes › Preferencias del dispositivo › Información › pulsa **7 veces** «Compilación»
   (o «Build») hasta que diga que eres desarrollador. Después, en Ajustes › Preferencias del dispositivo ›
   Opciones para desarrolladores, activa **Depuración USB** y, si aparece, **Depuración por red** / **ADB por red**.
   Anota la IP de la tele (Ajustes › Red).
4. La primera vez que el PC se conecte, la tele muestra «¿Permitir la depuración?»: marca «Permitir siempre» y
   acepta.

## El script

```bash
powershell -ExecutionPolicy Bypass -File scripts/limpiar-tv.ps1
```

(En Mac o Linux: `pwsh scripts/limpiar-tv.ps1`.)

Qué hace, en orden:

1. Se conecta a la tele, lee **marca, modelo y versión de Android** y dice si su marca tiene lista propia.
2. Lee los paquetes instalados y los cruza con el catálogo `scripts/catalogo-tv.json`. Solo propone lo que **está
   instalado en esa tele y todavía no está hecho**.
3. Muestra los grupos con cuántos cambios tiene cada uno. Escribe un número para marcar o desmarcar un grupo y
   `v` para ver el detalle paquete por paquete.
4. Pregunta si instalar el **Inicio** (sin publicidad) y **Pantalla** (receptor).
5. Pregunta si aplicar. **Si dices que no, no cambia nada.**
6. Al aplicar, deja en la carpeta actual `limpiar-tv-<marca>-<modelo>-<fecha>/` con la lista de paquetes de antes
   y un **`revertir.ps1`** que deshace exactamente lo que se hizo, en orden inverso.

| Grupo | Por defecto | Qué incluye |
|---|---|---|
| Telemetría y envío de datos | Sí | Servicios de recolección de datos del fabricante y de Google, y los ajustes que mandan datos (experiencia de usuario, subida de registros e informes, seguimiento publicitario) |
| Publicidad, recomendaciones y canales | Sí | Recomendaciones del inicio de Google, canales patrocinados, clientes de publicidad |
| Apps de fábrica poco usadas | No | Galería, Play Juegos, YouTube Music, sincronización de contactos y calendario, salvapantallas pesado… (**revísala**: TalkBack está aquí) |
| Apps de streaming preinstaladas | No | Netflix, Prime Video y Disney+ de fábrica (se reinstalan desde la Play Store) |
| Asistentes y actualizaciones del sistema | No | Asistentes de primera configuración y el servicio OTA. **Sin OTA la tele no se actualiza sola** |
| Interfaz más ágil y limpieza de caché | Sí | Animaciones a la mitad, limpiar caché, recompilar apps (tarda unos minutos) |

Lo que **nunca** toca: los servicios de Google (`gms`, `gsf`), la Play Store, YouTube, el control por voz,
Chromecast, los servicios base del fabricante y las apps que instalaste tú.

### Instalar el Inicio y Pantalla

El script baja las APK del **último release** de sus repositorios y las instala solo si pasan las mismas
comprobaciones que usan las apps para actualizarse (`docs/actualizaciones.md`): manifiesto **firmado** con la clave
del proyecto, del repositorio esperado, descarga desde sus releases y **tamaño y SHA-256 exactos**. Si algo no
cuadra, no instala nada. Con `-ApkInicio <ruta>` o `-ApkReceptor <ruta>` usa una APK que ya tengas (sin esas
comprobaciones: verifícala antes con `docs/verificar.md`).

Al instalar el Inicio lo deja como pantalla de inicio, desactiva los inicios de fábrica conocidos (Google TV y
PatchWall de Xiaomi) y pone su reloj como salvapantallas. Todo eso también queda en `revertir.ps1`.

### Sin preguntas

```bash
powershell -ExecutionPolicy Bypass -File scripts/limpiar-tv.ps1 -Ip 10.0.0.20 -Grupos telemetria,publicidad,rendimiento -Inicio -Aplicar -Si
```

Sin `-Aplicar`, `-Si` solo muestra lo que haría. Los ids de los grupos están en `scripts/catalogo-tv.json`.
`-TodasLasMarcas` aplica también las entradas de otras marcas que estén instaladas (útil en equipos con
componentes compartidos; úsalo con cuidado).

### Deshacer

```bash
powershell -ExecutionPolicy Bypass -File limpiar-tv-<marca>-<modelo>-<fecha>/revertir.ps1
```

Si perdiste la carpeta: un paquete quitado vuelve con `adb shell pm install-existing --user 0 <paquete>` y uno
desactivado con `adb shell pm enable --user 0 <paquete>`. Los paquetes de cada equipo probado están en
`docs/equipo-*.md`. Como último recurso, un restablecimiento de fábrica deja todo como venía.

### Cosas que pasan

- **Algunos ajustes vuelven solos.** La Xiaomi vuelve a poner en 1 `user_experience_enabled` y `upload_log_pref`
  **en cada arranque** (comprobado reiniciándola el 12/11/2026). Por eso el Inicio, desde la 0.5.0, los revisa al
  encender la tele y al volver a él, y vuelve a apagarlos: el script le da el permiso que necesita al instalarlo o
  al limpiar la telemetría (ver el README, «Mantener apagada la telemetría»). Sin el Inicio, corre el script de
  nuevo después de reiniciar: es idempotente y solo hace lo que falte. Las apps desactivadas no vuelven.
- **El inicio no cambia.** Algún fabricante tiene otro inicio que el script no conoce. Míralo con
  `adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME`,
  desactívalo con `adb shell pm disable-user --user 0 <paquete>` y, si funciona, súmalo a `inicios` en el catálogo.
- **«unauthorized»**: acepta el aviso de depuración en la tele.

## Sumar una marca

Si limpiaste una tele de otra marca, súmala al catálogo para que la próxima persona lo haga igual:

1. En `marcas`, una entrada con su patrón (lo que aparece en `getprop ro.product.manufacturer`, `ro.product.brand`
   o `ro.product.model`) y en qué equipo lo probaste.
2. Sus paquetes en el grupo que corresponda, con `"marca": "<id>"`, la acción (`quitar` o `desactivar`) y qué es.
3. Un `docs/equipo-<marca>.md` con lo que hiciste y cómo revertirlo, como los de Xiaomi y Philips.
4. Un pull request (ver `CONTRIBUTING.md`).

Para saber qué es un paquete: `adb shell dumpsys package <paquete> | grep -i -E "versionName|codePath"`, el nombre
que muestra en Ajustes › Apps y, si hace falta, buscarlo. **Ante la duda, desactiva en vez de quitar, y no toques
lo que no sabes qué es.**

## Los comandos a mano

Lo mismo que hace el script, por si prefieres ir paso a paso:

```bash
adb connect <IP de tu TV>:5555
adb shell getprop ro.product.manufacturer       # marca
adb shell pm list packages                      # lo instalado
adb shell pm list packages -d                   # lo desactivado
adb shell pm uninstall --user 0 <paquete>       # quitar (revertir: pm install-existing --user 0 <paquete>)
adb shell pm disable-user --user 0 <paquete>    # desactivar (revertir: pm enable --user 0 <paquete>)
adb shell settings get global user_experience_enabled      # anotar el valor antes de cambiarlo
adb shell settings put global user_experience_enabled 0
adb install -r inicio-<versión>.apk             # el Inicio (verifícalo antes: docs/verificar.md)
adb shell cmd package set-home-activity cl.itatagroup.launcher/.Home
adb shell pm trim-caches 999G
adb reboot
```

La lista completa de paquetes y ajustes, con qué es cada uno, está en `scripts/catalogo-tv.json`.
