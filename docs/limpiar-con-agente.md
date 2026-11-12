# Limpiar una tele con un agente de IA

La forma recomendada es el script determinista (`docs/limpiar-tv.md`): hace siempre lo mismo y deja un
`revertir.ps1`. Si tu tele es de una marca que el catálogo todavía no conoce, o prefieres que un agente con acceso
a la terminal (Claude Code, por ejemplo) te guíe y te explique cada paso, copia el prompt de abajo. Las reglas
están pensadas para que el agente **no improvise**: mire primero, proponga con opciones, aplique solo lo que
apruebes y deje todo reversible.

Necesitas lo mismo que para el script: ADB en el PC, la depuración activada en la tele y su IP
(`docs/limpiar-tv.md`, «Antes de empezar»).

## El prompt

```text
Quiero limpiar mi tele Android por ADB. La IP de la tele es <IP>. Trabaja así, sin saltarte pasos:

1. Solo lectura primero. Conéctate (adb connect <IP>:5555) y reúne: marca, modelo y versión de Android
   (getprop), memoria libre (dumpsys meminfo, resumen), lista de paquetes instalados y desactivados
   (pm list packages, pm list packages -d), el inicio actual (cmd package resolve-activity -a
   android.intent.action.MAIN -c android.intent.category.HOME) y los ajustes de envío de datos
   (settings get de user_experience_enabled, upload_log_pref, send_action_app_error, upload_apk_enable,
   limit_ad_tracking, network_recommendations_enabled). No cambies nada en este paso.

2. Usa como base el catálogo del proyecto launcher-tv (scripts/catalogo-tv.json, en
   https://github.com/itatagroup/launcher-tv): si la marca está ahí, propone exactamente sus entradas. Para lo que
   no esté en el catálogo, clasifica cada paquete del fabricante en: telemetría, publicidad, app de fábrica,
   esencial o desconocido. Explica en una línea qué es cada uno y de dónde lo sacas. Lo desconocido NO se toca.

3. Nunca propongas tocar: servicios de Google (com.google.android.gms, com.google.android.gsf), Play Store,
   YouTube, control por voz, Chromecast (mediashell), servicios base del fabricante (tvservice, framework,
   proveedores del sistema), el teclado, ni apps que instalé yo. Ante la duda, desactivar (pm disable-user) en vez
   de quitar (pm uninstall --user 0). Nunca uses pm uninstall sin --user 0 en una app del sistema, ni root.

4. Muéstrame una tabla por grupos (paquete, qué es, acción, cómo revertir) y pregúntame qué grupos aplicar. No
   apliques nada sin mi «sí» explícito.

5. Antes de aplicar, guarda en un archivo la lista de paquetes y los valores de los ajustes que vas a cambiar, y
   escribe un revertir.ps1 (o revertir.sh) con los comandos inversos, en orden inverso:
   pm install-existing --user 0 <p> para lo quitado, pm enable --user 0 <p> para lo desactivado y el valor
   anterior para cada ajuste (settings delete si era null).

6. Aplica uno por uno, mostrando el resultado de cada comando. Si uno falla, detente y pregúntame.

7. Si te pido el inicio sin publicidad o la app Pantalla: bájalas del último release de
   https://github.com/itatagroup/launcher-tv y https://github.com/itatagroup/pantalla-tv, comprueba el SHA-256
   contra SHA256SUMS.txt del release y el certificado con apksigner (huellas en docs/verificar.md de cada repo).
   Instala con adb install -r. Para el inicio: cmd package set-home-activity cl.itatagroup.launcher/.Home y
   desactiva los inicios de fábrica (anótalos en revertir).

8. Al terminar: comprueba que la tele arranca al inicio esperado, muestra la memoria libre antes y después, y
   dame un resumen con lo hecho y dónde quedó el revertir. Si limpiaste una marca que no estaba en el catálogo,
   escribe las entradas nuevas con el formato de catalogo-tv.json para que pueda proponerlas al proyecto.
```

## Después

Si el agente limpió una marca nueva y te dejó las entradas del catálogo, pruébalas con el script
(`scripts/limpiar-tv.ps1 -Catalogo <tu catálogo>`) y propónlas en un pull request: así la próxima persona con esa
tele lo hace sin agente.
