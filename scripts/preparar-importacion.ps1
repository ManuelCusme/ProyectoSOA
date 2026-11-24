param(
    [string]$RutaPhp = 'C:\xampp\php\php.exe',
    [string]$UsuarioBodeguero = 'bodeguero'
)
$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$rutaConfig = Join-Path $raizProyecto '.local/application.properties'
$rutaCredenciales = Join-Path $raizProyecto '.local/credenciales-importacion.properties'
$rutaSql = Join-Path $raizProyecto '.local/importar-inventario.sql'
$rutaSchema = Join-Path $raizProyecto 'src/main/resources/schema.sql'
if (-not (Test-Path -LiteralPath $RutaPhp)) {
    throw 'No se encontro PHP de XAMPP. Indique -RutaPhp con la ruta a php.exe.'
}
if (-not (Test-Path -LiteralPath $rutaConfig)) {
    throw 'Primero ejecute scripts/configurar-local.ps1 y revise las credenciales locales.'
}

# Lee valores de una sola linea, incluyendo escapes habituales de Java Properties.
function Leer-Propiedad([string]$Ruta, [string]$Nombre) {
    foreach ($linea in [IO.File]::ReadAllLines($Ruta, [Text.Encoding]::UTF8)) {
        if ($linea -match ('^\s*' + [regex]::Escape($Nombre) + '\s*=(.*)$')) {
            $valor = $Matches[1].TrimStart()
            if ($valor -match '^\$\{') { throw "Complete $Nombre con un valor local antes de exportar." }
            return [regex]::Replace($valor, '\\u([0-9a-fA-F]{4})|\\(.)', {
                param($coincidencia)
                if ($coincidencia.Groups[1].Success) {
                    return [string][char][Convert]::ToInt32($coincidencia.Groups[1].Value,16)
                }
                switch ($coincidencia.Groups[2].Value) {
                    'n' { return "`n" }; 'r' { return "`r" }
                    't' { return "`t" }; 'f' { return "`f" }
                    default { return $coincidencia.Groups[2].Value }
                }
            })
        }
    }
    throw "Falta la propiedad $Nombre en el archivo privado."
}
function Escapar-Propiedad([string]$Valor) {
    return $Valor.Replace('\','\\').Replace("`n",'\n').Replace("`r",'\r').Replace("`t",'\t').Replace(' ','\ ')
}

if (Test-Path -LiteralPath $rutaCredenciales) {
    # Regenerar el SQL conserva las claves anteriores, aunque cambie la sal BCrypt.
    $usuarioAdmin = Leer-Propiedad $rutaCredenciales 'admin.username'
    $claveAdmin = Leer-Propiedad $rutaCredenciales 'admin.password'
    $UsuarioBodeguero = Leer-Propiedad $rutaCredenciales 'bodeguero.username'
    $claveBodeguero = Leer-Propiedad $rutaCredenciales 'bodeguero.password'
} else {
    $usuarioAdmin = Leer-Propiedad $rutaConfig 'app.bootstrap.username'
    $claveAdmin = Leer-Propiedad $rutaConfig 'app.bootstrap.password'
    $bytesClave = New-Object byte[] 24
    $generador = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generador.GetBytes($bytesClave) } finally { $generador.Dispose() }
    $claveBodeguero = [Convert]::ToBase64String($bytesClave)
}
foreach ($usuario in @($usuarioAdmin, $UsuarioBodeguero)) {
    if ($usuario -notmatch '^[a-zA-Z0-9._-]{3,60}$') { throw 'Los usuarios deben tener de 3 a 60 letras, numeros, puntos o guiones.' }
}
if ($usuarioAdmin -ieq $UsuarioBodeguero) { throw 'El administrador y el bodeguero deben ser cuentas distintas.' }
foreach ($clave in @($claveAdmin, $claveBodeguero)) {
    if ($clave.Length -lt 10 -or [Text.Encoding]::UTF8.GetByteCount($clave) -gt 72) {
        throw 'Las claves requieren al menos 10 caracteres y como maximo 72 bytes UTF-8.'
    }
}

# Las claves viajan por stdin; nunca aparecen en argumentos ni en la consola.
$inicioPhp = New-Object Diagnostics.ProcessStartInfo
$inicioPhp.FileName = $RutaPhp
$inicioPhp.Arguments = '"' + (Join-Path $PSScriptRoot 'generar-hashes.php') + '"'
$inicioPhp.UseShellExecute = $false
$inicioPhp.CreateNoWindow = $true
$inicioPhp.RedirectStandardInput = $true
$inicioPhp.RedirectStandardOutput = $true
$inicioPhp.RedirectStandardError = $true
$procesoPhp = New-Object Diagnostics.Process
$procesoPhp.StartInfo = $inicioPhp
try {
    [void]$procesoPhp.Start()
    $entradaJson = @{admin=$claveAdmin; bodeguero=$claveBodeguero} | ConvertTo-Json -Compress
    $bytesEntrada = [Text.Encoding]::UTF8.GetBytes($entradaJson)
    $procesoPhp.StandardInput.BaseStream.Write($bytesEntrada, 0, $bytesEntrada.Length)
    $procesoPhp.StandardInput.BaseStream.Close()
    $salidaPhp = $procesoPhp.StandardOutput.ReadToEnd()
    $errorPhp = $procesoPhp.StandardError.ReadToEnd()
    $procesoPhp.WaitForExit()
    if ($procesoPhp.ExitCode -ne 0) { throw 'No se pudieron generar los hashes con PHP de XAMPP.' }
    $hashes = $salidaPhp | ConvertFrom-Json
} finally { $procesoPhp.Dispose() }
foreach ($hash in @($hashes.admin, $hashes.bodeguero)) {
    if ($hash -notmatch '^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$') { throw 'El generador no devolvio un hash BCrypt valido.' }
}

$cabeceraPrivada = @"
-- ARCHIVO PRIVADO: no publicar. Contiene hashes para las cuentas de esta instalacion.
SET @completar_ejemplos_iniciales = 1;
SET @usuario_admin_inicial = '$usuarioAdmin';
SET @hash_admin_inicial = '$($hashes.admin)';
SET @usuario_bodeguero_inicial = '$UsuarioBodeguero';
SET @hash_bodeguero_inicial = '$($hashes.bodeguero)';

"@
$sqlCompleto = $cabeceraPrivada + "`n" + [IO.File]::ReadAllText($rutaSchema, [Text.Encoding]::UTF8)
[IO.File]::WriteAllText($rutaSql, $sqlCompleto, (New-Object Text.UTF8Encoding($false)))
if (-not (Test-Path -LiteralPath $rutaCredenciales)) {
    $textoCredenciales = @"
# PRIVADO: claves iniciales para esta instalacion. No publicar.
admin.username=$usuarioAdmin
admin.password=$(Escapar-Propiedad $claveAdmin)
bodeguero.username=$UsuarioBodeguero
bodeguero.password=$(Escapar-Propiedad $claveBodeguero)
"@
    [IO.File]::WriteAllText($rutaCredenciales, $textoCredenciales, (New-Object Text.UTF8Encoding($false)))
}
Write-Host "SQL completo generado: $rutaSql"
Write-Host "Consulte las claves en el archivo privado: $rutaCredenciales"
Write-Host 'Importe el SQL generado en phpMyAdmin. Las cuentas existentes no se sobrescriben.'
