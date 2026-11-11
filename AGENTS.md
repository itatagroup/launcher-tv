# AGENTS.md — launcher-tv

Guía para agentes de IA y colaboradores.

## Idioma

Todo en **español de Chile**: respuestas, commits, documentación y textos de la pantalla. El código
sigue la convención Java/Android habitual.

## Qué es

Launcher Android TV minimalista (`cl.itatagroup.launcher`): favoritas, cuadrícula de todas las apps,
menú de mantener OK y salvapantallas de reloj. Sin dependencias externas, sin Gradle, sin AndroidX y
sin permiso de internet: se compila con `tools/build.mjs` (aapt2 + javac + d8 + apksigner). Detalle en
`README.md`. Licencia GPL-3.0.

## Ramas

`dev` es la rama de trabajo y `main` la estable. Los cambios entran por pull request contra `dev`.

## Comandos

- Compilar: `node tools/build.mjs` → `out/launcher.apk` (necesita la clave en `keystore/`, ver README).
- Instalar: `powershell -File scripts/instalar.ps1 -Ip <IP de tu TV>`.
- No hay pruebas automáticas. Antes de dar algo por terminado: compilar, instalar en una TV, comprobar
  que el inicio abre, lista las apps y lanza una (`docs/build-y-despliegue.md`, «Verificación manual»).

## Reglas

1. **La clave de firma no se versiona.** `keystore/` está en `.gitignore`; nunca la subas ni la imprimas.
2. **Sin dependencias.** Antes de sumar una, medir con `adb shell dumpsys meminfo cl.itatagroup.launcher`.
3. **Sin telemetría ni conexiones a internet nuevas.** No agregar el permiso `INTERNET`.
4. **Aspecto en recursos XML** (`res/`), no en código. Lo configurable en `res/values/config.xml`.
5. **Todo cambio que hagas en un equipo** (paquetes quitados, ajustes) se documenta en su doc de
   equipo con cómo revertirlo: `docs/equipo-xiaomi-mi-tv-stick.md` y `scripts/optimizar-tvbox.ps1`;
   `docs/equipo-philips-android-tv.md` y `scripts/optimizar-philips.ps1`.
6. Sube `versionCode` en `version.json` en cada cambio que se vaya a publicar.

## Qué falta

Pruebas automáticas (hoy solo verificación manual en el equipo).
