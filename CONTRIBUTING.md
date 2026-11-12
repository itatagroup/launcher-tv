# Cómo contribuir

¡Gracias por querer ayudar! Se aceptan issues y pull requests, **en español**.

## Flujo

1. Abre un issue si el cambio es grande, para conversarlo antes.
2. Haz tu rama desde `dev` y abre el pull request **contra `dev`** (`main` es la rama estable).
3. El mantenedor revisa y aprueba todo cambio antes de mezclarlo.

## Reglas del proyecto

- **Sin dependencias externas.** Cada librería agrega MB y RAM; el objetivo es lo contrario.
- **Sin telemetría ni conexiones a internet nuevas.** El launcher pide `INTERNET` y
  `REQUEST_INSTALL_PACKAGES` **solo** para la búsqueda manual de actualización del propio inicio
  (`Actualizador.java`, `docs/actualizaciones.md`): nunca se conecta si el usuario no la elige. No se acepta
  ninguna otra conexión, ni consultas automáticas.
- **El aspecto va en recursos XML** (`app/src/main/res/`), no en código. Lo configurable (columnas,
  favoritas de partida, paquetes ocultos) va en `res/values/config.xml`.
- **Nunca subas claves.** `keystore/` está en `.gitignore`; no la incluyas en ningún commit.
- Si tu cambio toca lo que se hace en un equipo (paquetes quitados, ajustes), documéntalo en su doc
  de equipo, con cómo revertirlo.
- Textos y documentación en español de Chile.

## Antes de abrir el PR

```bash
node tools/build.mjs
```

Debe terminar con «Listo». Si tocas `FirmaManifiesto.java`, corre también `node tools/probar-firma.mjs`
(prueba la verificación de firma en el PC con `javac`/`java`). Si puedes, prueba la APK en una TV
(`docs/build-y-despliegue.md`, «Verificación manual»). Fuera de eso no hay pruebas automáticas.

## Licencia

Al contribuir aceptas que tu aporte se publique bajo la licencia GPL-3.0 del proyecto.
