<#
  Limpia una tele Android (Android TV o Google TV) por ADB, de forma interactiva y reversible.
  Quita o desactiva telemetría, publicidad y apps de fábrica según scripts/catalogo-tv.json, ajusta la privacidad,
  y opcionalmente instala el Inicio (launcher sin publicidad) y Pantalla (receptor) desde sus releases firmados.

  Nada se borra del sistema: los paquetes se quitan solo para el usuario 0 o se desactivan. Antes de aplicar deja
  una carpeta con el plan y un revertir.ps1 que deshace exactamente lo que se hizo. Sin -Aplicar (y sin decir que sí
  al final) solo muestra lo que haría.

  Uso (Windows):      powershell -ExecutionPolicy Bypass -File scripts/limpiar-tv.ps1
  Uso (Mac/Linux):    pwsh scripts/limpiar-tv.ps1
  Sin preguntas:      ... -Ip 10.0.0.20 -Grupos telemetria,publicidad,rendimiento -Inicio -Aplicar -Si
  Guía completa: docs/limpiar-tv.md
#>
param(
  [string]$Ip,                       # IP de la tele (o serie de adb). Sin ella, se pregunta.
  [string[]]$Grupos,                 # ids de grupos del catálogo. Sin ellos, se pregunta.
  [switch]$Inicio,                   # instalar el Inicio y dejarlo como pantalla de inicio
  [switch]$Receptor,                 # instalar Pantalla (receptor)
  [string]$ApkInicio,                # usar esta APK local en vez del release
  [string]$ApkReceptor,
  [switch]$TodasLasMarcas,           # aplicar también entradas de otras marcas (solo si están instaladas)
  [switch]$Aplicar,                  # aplicar sin la pregunta final de confirmación... (ver -Si)
  [switch]$Si,                       # ...y sin ninguna pregunta
  [string]$Catalogo = (Join-Path $PSScriptRoot "catalogo-tv.json")
)
$ErrorActionPreference = "Stop"
$esCore = $PSVersionTable.PSEdition -eq "Core"
if (-not $esCore) { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 }

function Titulo($t) { Write-Host ""; Write-Host "== $t" -ForegroundColor Cyan }
function Preguntar($texto, $porDefecto = "") {
  if ($Si) { return $porDefecto }
  $r = Read-Host $texto
  if ([string]::IsNullOrWhiteSpace($r)) { return $porDefecto } else { return $r.Trim() }
}
function SiNo($texto, [bool]$porDefecto) {
  if ($Si) { return $porDefecto }
  $marca = if ($porDefecto) { "S/n" } else { "s/N" }
  $r = Read-Host "$texto [$marca]"
  if ([string]::IsNullOrWhiteSpace($r)) { return $porDefecto }
  return $r.Trim().ToLower().StartsWith("s")
}

# ── adb ─────────────────────────────────────────────────────────────────────────────────────────
$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb -and $env:LOCALAPPDATA) {
  $candidato = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
  if (Test-Path $candidato) { $adb = $candidato }
}
if (-not $adb) { throw "No encuentro adb. Instala las «Android SDK Platform-Tools» y ponlas en el PATH (ver docs/limpiar-tv.md)." }

# En Windows PowerShell 5.1 el stderr de adb con «Stop» abortaría todo: se corre con «Continue».
function Adb([string[]]$argumentos) {
  $previo = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try { $salida = & $adb @argumentos 2>&1 } finally { $ErrorActionPreference = $previo }
  return (($salida | ForEach-Object { "$_" }) -join "`n").Trim()
}
function Shell([string]$comando) { return Adb @("-s", $script:serie, "shell", $comando) }

$catalogoDatos = Get-Content -Raw -Encoding UTF8 $Catalogo | ConvertFrom-Json
if ($catalogoDatos.formato -ne 1) { throw "Catálogo con formato desconocido: $Catalogo" }

# ── Conectar ────────────────────────────────────────────────────────────────────────────────────
Titulo "Conectar con la tele"
if (-not $Ip) {
  $conectados = (Adb @("devices")) -split "`n" | Where-Object { $_ -match "\sdevice$" } | ForEach-Object { ($_ -split "\s+")[0] }
  if ($conectados) { Write-Host "Ya conectadas: $($conectados -join ', ')" }
  $Ip = Preguntar "IP de la tele (en la tele: Ajustes › Red), o la serie de una conectada" ""
  if (-not $Ip) { throw "Falta la IP de la tele." }
}
$script:serie = if ($Ip -match "^\d+\.\d+\.\d+\.\d+$") { "${Ip}:5555" } else { $Ip }
if ($script:serie -match ":\d+$") { Adb @("connect", $script:serie) | Out-Null }
$estado = ((Adb @("devices")) -split "`n" | Where-Object { $_.StartsWith($script:serie) }) -join ""
if ($estado -match "unauthorized") { throw "La tele no autorizó la conexión: acepta el aviso de depuración en su pantalla y vuelve a correr el script." }
if ($estado -notmatch "\sdevice") { throw "Sin conexión con $($script:serie). ¿Activaste la depuración por red? (docs/limpiar-tv.md)" }

$fabricante = Shell "getprop ro.product.manufacturer"
$marcaProp = Shell "getprop ro.product.brand"
$modelo = Shell "getprop ro.product.model"
$android = Shell "getprop ro.build.version.release"
$texto = "$fabricante $marcaProp $modelo".ToLower()
$marcas = @()
foreach ($p in $catalogoDatos.marcas.PSObject.Properties) { if ($texto -match $p.Value.patron) { $marcas += $p.Name } }
Write-Host "Tele: $fabricante $modelo (marca $marcaProp), Android $android"
if ($marcas) {
  foreach ($m in $marcas) { $i = $catalogoDatos.marcas.$m; Write-Host "Marca reconocida: $($i.nombre) (probado en: $($i.probado))" -ForegroundColor Green }
} else {
  Write-Host "Marca sin lista propia todavía: se aplica solo lo genérico y seguro para cualquier Android TV." -ForegroundColor Yellow
}

$instalados = @{}
foreach ($l in (Shell "pm list packages") -split "`n") { if ($l -match "^package:(.+)$") { $instalados[$Matches[1].Trim()] = $true } }
$desactivados = @{}
foreach ($l in (Shell "pm list packages -d") -split "`n") { if ($l -match "^package:(.+)$") { $desactivados[$Matches[1].Trim()] = $true } }

function Aplica($entrada) { return $TodasLasMarcas -or $entrada.marca -eq "todas" -or $marcas -contains $entrada.marca }

# ── Plan por grupo: solo lo que existe en esta tele y todavía no está hecho ─────────────────────
$planes = @()
foreach ($g in $catalogoDatos.grupos) {
  $acciones = @()
  foreach ($e in @($g.paquetes)) {
    if (-not $e -or -not (Aplica $e) -or -not $instalados.ContainsKey($e.paquete)) { continue }
    if ($e.accion -eq "desactivar" -and $desactivados.ContainsKey($e.paquete)) { continue }
    $acciones += [pscustomobject]@{ Tipo = $e.accion; Paquete = $e.paquete; Que = $e.que }
  }
  foreach ($a in @($g.ajustes)) {
    if (-not $a) { continue }
    $actual = Shell "settings get $($a.espacio) $($a.clave)"
    if ($actual -eq $a.valor) { continue }
    $acciones += [pscustomobject]@{ Tipo = "ajuste"; Espacio = $a.espacio; Clave = $a.clave; Valor = $a.valor; Antes = $actual; Que = $a.que }
  }
  foreach ($c in @($g.comandos)) { if ($c) { $acciones += [pscustomobject]@{ Tipo = "comando"; Comando = $c.comando; Que = $c.que } } }
  $planes += [pscustomobject]@{ Grupo = $g; Acciones = $acciones; Elegido = [bool]$g.porDefecto }
}

# ── Elegir grupos ───────────────────────────────────────────────────────────────────────────────
Titulo "Qué limpiar"
if ($Grupos) {
  # Con «powershell -File», «-Grupos a,b» llega como un solo texto: se separa por comas.
  $Grupos = @($Grupos | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
  foreach ($p in $planes) { $p.Elegido = $Grupos -contains $p.Grupo.id }
} elseif (-not $Si) {
  while ($true) {
    for ($i = 0; $i -lt $planes.Count; $i++) {
      $p = $planes[$i]
      $x = if ($p.Elegido) { "[x]" } else { "[ ]" }
      Write-Host ("{0} {1}. {2} — {3} cambio(s)" -f $x, ($i + 1), $p.Grupo.nombre, $p.Acciones.Count)
      Write-Host ("       {0}" -f $p.Grupo.descripcion) -ForegroundColor DarkGray
    }
    $r = Read-Host "Escribe un número para marcar o desmarcar, 'v' para ver el detalle, o Enter para seguir"
    if ([string]::IsNullOrWhiteSpace($r)) { break }
    if ($r -eq "v") {
      foreach ($p in $planes) {
        Write-Host "-- $($p.Grupo.nombre)"
        foreach ($a in $p.Acciones) {
          $detalle = switch ($a.Tipo) { "ajuste" { "$($a.Espacio) $($a.Clave) = $($a.Valor)" } "comando" { $a.Comando } default { $a.Paquete } }
          Write-Host ("   {0,-10} {1}  ({2})" -f $a.Tipo, $detalle, $a.Que)
        }
        if (-not $p.Acciones) { Write-Host "   (nada que hacer en esta tele)" }
      }
      continue
    }
    $n = 0
    if ([int]::TryParse($r, [ref]$n) -and $n -ge 1 -and $n -le $planes.Count) { $planes[$n - 1].Elegido = -not $planes[$n - 1].Elegido }
  }
}
if (-not $PSBoundParameters.ContainsKey("Inicio") -and -not $Si) {
  $Inicio = SiNo "¿Instalar el Inicio sin publicidad (cl.itatagroup.launcher) y dejarlo como inicio?" $false
}
if (-not $PSBoundParameters.ContainsKey("Receptor") -and -not $Si) {
  $Receptor = SiNo "¿Instalar Pantalla, para ver el PC o el teléfono en esta tele?" $false
}

$elegidas = @($planes | Where-Object { $_.Elegido } | ForEach-Object { $_.Acciones })
# Guardián de privacidad del Inicio (desde la 0.5.0): algunas teles vuelven a encender los ajustes de envío de
# datos en cada arranque, y el Inicio los vuelve a apagar si tiene WRITE_SECURE_SETTINGS, que Android solo deja
# dar por ADB. Se da al instalar el Inicio, o si ya está y se eligió limpiar la telemetría y todavía no lo tiene.
$paqueteInicio = $catalogoDatos.apps.inicio.paquete
$conTelemetria = @($planes | Where-Object { $_.Elegido -and $_.Grupo.id -eq "telemetria" }).Count -gt 0
$yaTienePermiso = $instalados.ContainsKey($paqueteInicio) -and
  ((Shell "dumpsys package $paqueteInicio") -match "WRITE_SECURE_SETTINGS: granted=true")
$darPermiso = $Inicio -or ($conTelemetria -and $instalados.ContainsKey($paqueteInicio) -and -not $yaTienePermiso)
Titulo "Resumen"
Write-Host "$($elegidas.Count) cambio(s) de limpieza$(if ($Inicio) { ' + instalar el Inicio' })$(if ($Receptor) { ' + instalar Pantalla' })$(if ($darPermiso -and -not $Inicio) { ' + que el Inicio mantenga apagada la telemetría' })."
if (-not $elegidas -and -not $Inicio -and -not $Receptor -and -not $darPermiso) { Write-Host "Nada que hacer."; return }
if (-not $Aplicar) {
  if ($Si) { Write-Host "Simulación (agrega -Aplicar para hacerlo)."; $elegidas | Format-Table Tipo, Paquete, Clave, Valor, Que -AutoSize | Out-Host; return }
  $Aplicar = SiNo "¿Aplicar ahora? (queda un revertir.ps1 para deshacerlo)" $false
  if (-not $Aplicar) { Write-Host "No se cambió nada."; return }
}

# ── Respaldo y revertir.ps1 ─────────────────────────────────────────────────────────────────────
$nombreTele = ("$fabricante-$modelo" -replace "[^A-Za-z0-9_-]", "_")
$carpeta = Join-Path (Get-Location) ("limpiar-tv-{0}-{1}" -f $nombreTele, (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory $carpeta | Out-Null
(Shell "pm list packages") | Set-Content (Join-Path $carpeta "paquetes-antes.txt") -Encoding UTF8
(Shell "pm list packages -d") | Set-Content (Join-Path $carpeta "desactivados-antes.txt") -Encoding UTF8
$revertir = New-Object System.Collections.Generic.List[string]
function Revertir($comando) { $revertir.Insert(0, "Shell '$($comando -replace "'", "''")'") }

function Paso($texto, $comando) {
  $r = Shell $comando
  $ok = -not ($r -match "(?i)failure|error|exception|not installed|unknown package")
  $color = if ($ok) { "Gray" } else { "Yellow" }
  Write-Host ("{0} -> {1}" -f $texto, $(if ($r) { $r } else { "ok" })) -ForegroundColor $color
  return $ok
}

Titulo "Aplicando"
foreach ($a in $elegidas) {
  switch ($a.Tipo) {
    "quitar"     { if (Paso "quitar $($a.Paquete)" "pm uninstall --user 0 $($a.Paquete)") { Revertir "pm install-existing --user 0 $($a.Paquete)" } }
    "desactivar" { if (Paso "desactivar $($a.Paquete)" "pm disable-user --user 0 $($a.Paquete)") { Revertir "pm enable --user 0 $($a.Paquete)" } }
    "ajuste" {
      if (Paso "$($a.Que): $($a.Clave) = $($a.Valor)" "settings put $($a.Espacio) $($a.Clave) $($a.Valor)") {
        if ($a.Antes -eq "null" -or -not $a.Antes) { Revertir "settings delete $($a.Espacio) $($a.Clave)" }
        else { Revertir "settings put $($a.Espacio) $($a.Clave) $($a.Antes)" }
      }
    }
    "comando" { Paso $a.Que $a.Comando | Out-Null }
  }
}

# ── Instalar el Inicio y Pantalla desde sus releases firmados ───────────────────────────────────
function FirmaValida([byte[]]$datos, [string]$firmaB64, [string]$spkiB64) {
  # Cualquier problema (firma o clave mal formadas) es lo mismo que «no válida».
  try { return [bool](FirmaValidaSinCapturar $datos $firmaB64 $spkiB64) } catch { return $false }
}

function FirmaValidaSinCapturar([byte[]]$datos, [string]$firmaB64, [string]$spkiB64) {
  $firma = [Convert]::FromBase64String($firmaB64.Trim())
  $spki = [Convert]::FromBase64String($spkiB64)
  if ($esCore) {
    $ec = [Security.Cryptography.ECDsa]::Create()
    $leidos = 0
    $ec.ImportSubjectPublicKeyInfo($spki, [ref]$leidos)
    return $ec.VerifyData($datos, $firma, [Security.Cryptography.HashAlgorithmName]::SHA256, [Security.Cryptography.DSASignatureFormat]::Rfc3279DerSequence)
  }
  # Windows PowerShell 5.1 (.NET Framework): clave en formato ECCPUBLICBLOB y firma en r||s.
  $punto = $spki[($spki.Length - 64)..($spki.Length - 1)]
  $blob = [byte[]](0x45, 0x43, 0x53, 0x31, 32, 0, 0, 0) + $punto
  $clave = [Security.Cryptography.CngKey]::Import($blob, [Security.Cryptography.CngKeyBlobFormat]::EccPublicBlob)
  $ec = New-Object Security.Cryptography.ECDsaCng($clave)
  $ec.HashAlgorithm = [Security.Cryptography.CngAlgorithm]::Sha256
  # DER: 30 len 02 lenR R 02 lenS S
  $i = 2; if ($firma[1] -band 0x80) { $i += ($firma[1] -band 0x7f) }
  $partes = @()
  for ($k = 0; $k -lt 2; $k++) {
    $largo = $firma[$i + 1]; $valor = $firma[($i + 2)..($i + 1 + $largo)]; $i += 2 + $largo
    while ($valor.Count -gt 32 -and $valor[0] -eq 0) { $valor = $valor[1..($valor.Count - 1)] }
    $partes += ,([byte[]](@(0) * (32 - $valor.Count)) + [byte[]]$valor)
  }
  return $ec.VerifyData($datos, [byte[]]($partes[0] + $partes[1]))
}

function Bajar([string]$url, [string]$destino) {
  Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $destino -Headers @{ "User-Agent" = "limpiar-tv" } | Out-Null
}

function InstalarApp($cual, [string]$apkLocal) {
  $info = $catalogoDatos.apps.$cual
  $tmp = Join-Path ([IO.Path]::GetTempPath()) ("limpiar-tv-" + [guid]::NewGuid())
  New-Item -ItemType Directory $tmp | Out-Null
  try {
    if ($apkLocal) {
      $apk = $apkLocal
      Write-Host "Usando la APK local $apk (sin comprobar firma del manifiesto)." -ForegroundColor Yellow
    } else {
      # Las mismas reglas que usan las apps (docs/actualizaciones.md): firma, repo, URL del release, tamaño y SHA-256.
      Bajar $info.manifiesto (Join-Path $tmp "m.json")
      Bajar ($info.manifiesto + ".sig") (Join-Path $tmp "m.sig")
      $datos = [IO.File]::ReadAllBytes((Join-Path $tmp "m.json"))
      if (-not (FirmaValida $datos ([IO.File]::ReadAllText((Join-Path $tmp "m.sig"))) $catalogoDatos.clavePublica)) { throw "La firma del manifiesto de $($info.nombre) no es válida: no se instala." }
      $m = [Text.Encoding]::UTF8.GetString($datos) | ConvertFrom-Json
      if ($m.formato -ne 1 -or $m.repo -ne $info.repo) { throw "Manifiesto de otro repositorio." }
      $e = $m.apps.($info.app)
      $prefijo = "https://github.com/$($info.repo)/releases/download/"
      if (-not $e.url.StartsWith($prefijo) -or $e.url -match "\.\.|%|\?|#") { throw "La descarga no es de los releases de $($info.repo)." }
      $apk = Join-Path $tmp "app.apk"
      Write-Host "Descargando $($info.nombre) $($e.version)…"
      Bajar $e.url $apk
      if ((Get-Item $apk).Length -ne [long]$e.tamano) { throw "El tamaño de la APK no coincide con el manifiesto." }
      if ((Get-FileHash $apk -Algorithm SHA256).Hash.ToLower() -ne $e.sha256) { throw "El SHA-256 de la APK no coincide con el manifiesto." }
      Write-Host "Verificada: firma del manifiesto, tamaño y SHA-256." -ForegroundColor Green
    }
    $yaEstaba = $instalados.ContainsKey($info.paquete)
    $r = Adb @("-s", $script:serie, "install", "-r", $apk)
    if ($r -notmatch "Success") { throw "No se pudo instalar $($info.nombre): $r" }
    Write-Host "Instalada: $($info.nombre)."
    if (-not $yaEstaba) { Revertir "pm uninstall $($info.paquete)" }
  } finally {
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
  }
}

if ($Inicio) {
  Titulo "Inicio"
  InstalarApp "inicio" $ApkInicio
  $paquete = $catalogoDatos.apps.inicio.paquete
  Shell "cmd package set-home-activity $paquete/.Home" | Out-Null
  # Fijarlo no basta mientras otro inicio de mayor prioridad siga activo: se desactivan los de fábrica conocidos.
  foreach ($otro in $catalogoDatos.inicios) {
    if ($instalados.ContainsKey($otro.paquete) -and -not $desactivados.ContainsKey($otro.paquete)) {
      if (Paso "desactivar $($otro.que)" "pm disable-user --user 0 $($otro.paquete)") { Revertir "pm enable --user 0 $($otro.paquete)" }
    }
  }
  # El salvapantallas: el reloj del Inicio.
  foreach ($par in @(@("screensaver_components", "$paquete/.ClockDream"), @("screensaver_enabled", "1"))) {
    $antes = Shell "settings get secure $($par[0])"
    if ($antes -ne $par[1] -and (Paso "salvapantallas: $($par[0])" "settings put secure $($par[0]) $($par[1])")) {
      if ($antes -eq "null" -or -not $antes) { Revertir "settings delete secure $($par[0])" } else { Revertir "settings put secure $($par[0]) $antes" }
    }
  }
  Shell "input keyevent KEYCODE_HOME" | Out-Null
  Start-Sleep -Seconds 2
  $inicioActual = Shell "cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME"
  if ($inicioActual -match [regex]::Escape($paquete)) { Write-Host "El Inicio quedó como pantalla de inicio." -ForegroundColor Green }
  else { Write-Host "Instalado, pero el inicio sigue siendo otro: revisa docs/limpiar-tv.md («El inicio no cambia»)." -ForegroundColor Yellow }
}
if ($Receptor) { Titulo "Pantalla"; InstalarApp "receptor" $ApkReceptor }

if ($darPermiso) {
  Titulo "Mantener apagada la telemetría"
  $paqueteInicio = $catalogoDatos.apps.inicio.paquete
  if (Paso "dar al Inicio el permiso para mantener apagados los ajustes de envío de datos" "pm grant $paqueteInicio android.permission.WRITE_SECURE_SETTINGS") {
    Revertir "pm revoke $paqueteInicio android.permission.WRITE_SECURE_SETTINGS"
    Write-Host "El Inicio los revisará al encender la tele y al volver a él (se ve y se apaga con el botón «Telemetría» junto a la hora)." -ForegroundColor Green
  } else {
    Write-Host "Este Inicio no tiene el guardián (es anterior a la 0.5.0): actualízalo para usarlo." -ForegroundColor Yellow
  }
}

# ── revertir.ps1 ────────────────────────────────────────────────────────────────────────────────
$rutaRevertir = Join-Path $carpeta "revertir.ps1"
$cabecera = @"
<#
  Deshace lo que hizo limpiar-tv.ps1 el $(Get-Date -Format "dd-MM-yyyy HH:mm") en $fabricante $modelo ($($script:serie)).
  Uso: powershell -ExecutionPolicy Bypass -File revertir.ps1 [-Serie <ip:5555>]
#>
param([string]`$Serie = "$($script:serie)")
`$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not `$adb -and `$env:LOCALAPPDATA) { `$adb = Join-Path `$env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe" }
if (`$Serie -match ":\d+$") { & `$adb connect `$Serie | Out-Null }
function Shell([string]`$c) { Write-Host "`$c -> `$((& `$adb -s `$Serie shell `$c 2>&1) -join ' ')" }
"@
[IO.File]::WriteAllText($rutaRevertir, $cabecera + "`r`n" + ($revertir -join "`r`n") + "`r`n", (New-Object Text.UTF8Encoding($true)))

Titulo "Listo"
Write-Host "Respaldo y deshacer en: $carpeta"
Write-Host "Para deshacerlo todo: powershell -ExecutionPolicy Bypass -File `"$rutaRevertir`""
Write-Host "Reinicia la tele para que los cambios terminen de aplicarse (adb -s $($script:serie) reboot)."
