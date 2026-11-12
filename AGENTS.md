# AGENTS.md — launcher-tv

Guía para agentes de IA y colaboradores.

## Idioma

Todo en **español de Chile**: respuestas, commits, documentación y textos de la pantalla. El código
sigue la convención Java/Android habitual.

## Qué es

Launcher Android TV minimalista (`cl.itatagroup.launcher`): favoritas, cuadrícula de todas las apps,
menú de mantener OK, salvapantallas de reloj y búsqueda manual de actualización del propio inicio. Sin
dependencias externas, sin Gradle y sin AndroidX: se compila con `tools/build.mjs` (aapt2 + javac + d8 +
apksigner). Detalle en `README.md`. Licencia GPL-3.0.

## Ramas

`dev` es la rama de trabajo y `main` la estable. Los cambios entran por pull request contra `dev`.

## Comandos

- Compilar: `node tools/build.mjs` → `out/launcher.apk` (necesita la clave en `keystore/`, ver README).
- Instalar: `powershell -File scripts/instalar.ps1 -Ip <IP de tu TV>`.
- Probar la verificación de firma del actualizador en el PC: `node tools/probar-firma.mjs` (javac/java).
- Publicar: `powershell -File scripts/publicar.ps1 -Version <x.y.z>` → `dist/publicar/v<x.y.z>/` (ignorado) y
  el comando `gh release create ... --draft` impreso, que se corre a mano (`docs/actualizaciones.md`).
- Fuera de eso no hay pruebas automáticas. Antes de dar algo por terminado: compilar, instalar en una TV,
  comprobar que el inicio abre, lista las apps y lanza una (`docs/build-y-despliegue.md`, «Verificación manual»).

## Reglas

1. **Las claves no se versionan.** `keystore/` (la de firma del APK y `actualizaciones.pem`, la del
   manifiesto) está en `.gitignore`; nunca las subas, las imprimas ni las copies fuera de ahí.
2. **Sin dependencias.** Antes de sumar una, medir con `adb shell dumpsys meminfo cl.itatagroup.launcher`.
3. **Sin telemetría ni conexiones a internet nuevas.** `INTERNET` y `REQUEST_INSTALL_PACKAGES` existen
   solo para el botón «Buscar actualización», que el usuario pulsa a mano: el inicio **nunca consulta
   solo**. No agregar otras conexiones ni consultas automáticas.
4. **Aspecto en recursos XML** (`res/`), no en código. Lo configurable en `res/values/config.xml`.
5. **Todo cambio que hagas en un equipo** (paquetes quitados, ajustes) se documenta en su doc de
   equipo con cómo revertirlo (`docs/equipo-xiaomi-mi-tv-stick.md`, `docs/equipo-philips-android-tv.md`) y,
   si sirve para otros, en el catálogo `scripts/catalogo-tv.json` que usa `scripts/limpiar-tv.ps1`. La limpieza
   es determinista: lo que hace está en el catálogo, no en el criterio de quien la corre.
6. Sube `versionCode` en `version.json` en cada cambio que se vaya a publicar.
7. `Actualizador.java`, `FirmaManifiesto.java` y `ResultadoInstalacion.java` son copia de los de
   `pantalla-tv/android-comun/java/cl/itatagroup/actualizacion/`: un arreglo en uno va también al otro.

## Qué falta

Pruebas automáticas de la app (hoy solo `tools/probar-firma.mjs` para la firma y verificación manual en
el equipo).
