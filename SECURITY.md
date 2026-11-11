# Política de seguridad

## Cómo reportar una vulnerabilidad

**No abras un issue público.** Usa «Report a vulnerability» en la pestaña *Security* de este
repositorio de GitHub (reporte privado). Cuenta qué encontraste, cómo reproducirlo y qué versión
(`version.json`) es. Se responde lo antes posible y se coordina la publicación del arreglo.

## Qué está en alcance

- El código del launcher y del salvapantallas (`app/`).
- El proceso de compilación y firma (`tools/build.mjs`).
- Los scripts de instalación y limpieza (`scripts/`), por ejemplo si ejecutan algo no esperado.
- Que una APK publicada no corresponda al código fuente, o que pida permisos que el código no declara.

## Qué no está en alcance

- Vulnerabilidades del sistema operativo Android o del firmware de cada fabricante.
- Problemas de apps de terceros listadas por el launcher.
- Cambios en la TV hechos por el usuario con los scripts opcionales de limpieza, que son reversibles
  y están documentados.

El launcher no tiene permiso de internet, no envía datos y no guarda cuentas; aun así, cualquier
hallazgo que contradiga eso es de máxima prioridad.
