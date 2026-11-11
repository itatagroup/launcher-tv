# Cómo contribuir

¡Gracias por querer ayudar! Se aceptan issues y pull requests, **en español**.

## Flujo

1. Abre un issue si el cambio es grande, para conversarlo antes.
2. Haz tu rama desde `dev` y abre el pull request **contra `dev`** (`main` es la rama estable).
3. El mantenedor revisa y aprueba todo cambio antes de mezclarlo.

## Reglas del proyecto

- **Sin dependencias externas.** Cada librería agrega MB y RAM; el objetivo es lo contrario.
- **Sin telemetría ni conexiones a internet nuevas.** Hoy el launcher no pide el permiso `INTERNET`.
  La única excepción prevista es la búsqueda de actualizaciones, opcional y que el usuario puede apagar.
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

Debe terminar con «Listo». Si puedes, prueba la APK en una TV (`docs/build-y-despliegue.md`,
«Verificación manual»). No hay pruebas automáticas todavía.

## Licencia

Al contribuir aceptas que tu aporte se publique bajo la licencia GPL-3.0 del proyecto.
