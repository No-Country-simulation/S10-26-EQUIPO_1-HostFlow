$projectPath = Get-Location

Clear-Host
Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  VALIDADOR UNIVERSAL DE ARQUITECTURA SPRING BOOT" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Proyecto: $projectPath" -ForegroundColor Yellow
Write-Host ""

$errores = @()
$advertencias = @()
$correctos = @()

# ============================================================================
# 1. DETECTAR AUTOMATICAMENTE EL PAQUETE BASE
# ============================================================================
Write-Host "1. Detectando paquete base del proyecto..." -ForegroundColor Cyan
Write-Host ""

$javaSrcPath = Join-Path $projectPath "src\main\java"
if (-not (Test-Path $javaSrcPath)) {
    Write-Host "  [ERROR] No se encontro la carpeta src\main\java" -ForegroundColor Red
    Write-Host "  Este script solo funciona en proyectos Maven/Gradle estandar." -ForegroundColor Red
    Write-Host ""
    Write-Host "Presiona cualquier tecla para salir..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit
}

# Buscar el archivo Application.java para detectar el paquete base
$appClass = Get-ChildItem -Path $javaSrcPath -Recurse -Filter "*Application.java" -File -ErrorAction SilentlyContinue | Select-Object -First 1

if ($appClass) {
    $contenido = Get-Content -Path $appClass.FullName -Raw -Encoding UTF8
    if ($contenido -match "package\s+([\w\.]+);") {
        $paqueteBase = $matches[1]
        Write-Host "  [OK] Paquete base detectado: $paqueteBase" -ForegroundColor Green
        Write-Host "  [OK] Clase principal: $($appClass.Name)" -ForegroundColor Green
    } else {
        Write-Host "  [WARN] No se pudo detectar el paquete base desde Application.java" -ForegroundColor Yellow
        $paqueteBase = ""
    }
} else {
    Write-Host "  [WARN] No se encontro clase *Application.java" -ForegroundColor Yellow
    $paqueteBase = ""
}

# Construir ruta base del paquete
if ($paqueteBase) {
    $basePath = Join-Path $javaSrcPath ($paqueteBase -replace '\.', '\')
} else {
    # Si no se detecto, buscar subcarpetas comunes
    $subcarpetas = Get-ChildItem -Path $javaSrcPath -Directory -ErrorAction SilentlyContinue
    if ($subcarpetas.Count -gt 0) {
        $primerNivel = $subcarpetas[0].FullName
        $segundoNivel = Get-ChildItem -Path $primerNivel -Directory -ErrorAction SilentlyContinue
        if ($segundoNivel.Count -gt 0) {
            $basePath = $segundoNivel[0].FullName
            Write-Host "  [INFO] Paquete base inferido: $basePath" -ForegroundColor Gray
        } else {
            $basePath = $primerNivel
        }
    } else {
        $basePath = $javaSrcPath
    }
}

Write-Host ""

# ============================================================================
# 2. VERIFICAR ESTRUCTURA DE PAQUETES (busca en cualquier subcarpeta)
# ============================================================================
Write-Host "2. Verificando estructura de paquetes..." -ForegroundColor Cyan
Write-Host ""

$paquetesObligatorios = @("controller", "service", "repository")
$paquetesRecomendados = @("model", "dto", "entity", "config", "exception", "enums", "security", "util", "common")

foreach ($paquete in $paquetesObligatorios) {
    $encontrado = Get-ChildItem -Path $javaSrcPath -Recurse -Directory -Filter $paquete -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($encontrado) {
        $archivos = Get-ChildItem -Path $encontrado.FullName -Recurse -Filter "*.java" -File
        Write-Host "  [OK] Paquete '$paquete' encontrado en: $($encontrado.FullName.Replace($projectPath, ''))" -ForegroundColor Green
        Write-Host "       Contiene $($archivos.Count) archivo(s)" -ForegroundColor Green
        $correctos += "Paquete $paquete"
    } else {
        Write-Host "  [ERROR] Paquete '$paquete' NO ENCONTRADO (obligatorio en Spring Boot)" -ForegroundColor Red
        $errores += "Paquete $paquete no existe"
    }
}

Write-Host ""
Write-Host "  Paquetes recomendados (opcionales):" -ForegroundColor Gray
foreach ($paquete in $paquetesRecomendados) {
    $encontrado = Get-ChildItem -Path $javaSrcPath -Recurse -Directory -Filter $paquete -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($encontrado) {
        $archivos = Get-ChildItem -Path $encontrado.FullName -Recurse -Filter "*.java" -File
        Write-Host "    [OK] $paquete ($($archivos.Count) archivos)" -ForegroundColor Green
    } else {
        Write-Host "    [--] $paquete (no encontrado)" -ForegroundColor DarkGray
    }
}

Write-Host ""

# ============================================================================
# 3. VERIFICAR CONVENCIONES DE NOMBRES
# ============================================================================
Write-Host "3. Verificando convenciones de nombres..." -ForegroundColor Cyan
Write-Host ""

$controladores = Get-ChildItem -Path $javaSrcPath -Recurse -Filter "*.java" -File -ErrorAction SilentlyContinue | Where-Object {
    $_.FullName -like "*\controller\*" -or $_.FullName -like "*\controllers\*"
}

foreach ($ctrl in $controladores) {
    if ($ctrl.BaseName -notlike "*Controller" -and $ctrl.BaseName -notlike "*RestController") {
        Write-Host "  [WARN] '$($ctrl.Name)' esta en paquete controller pero no termina en 'Controller'" -ForegroundColor Yellow
        $advertencias += "$($ctrl.Name) no sigue convencion"
    }
}

$servicios = Get-ChildItem -Path $javaSrcPath -Recurse -Filter "*.java" -File -ErrorAction SilentlyContinue | Where-Object {
    $_.FullName -like "*\service\*" -or $_.FullName -like "*\services\*"
}

foreach ($svc in $servicios) {
    if ($svc.BaseName -notlike "*Service" -and $svc.BaseName -notlike "*ServiceImpl") {
        Write-Host "  [WARN] '$($svc.Name)' esta en paquete service pero no termina en 'Service'" -ForegroundColor Yellow
        $advertencias += "$($svc.Name) no sigue convencion"
    }
}

$repositorios = Get-ChildItem -Path $javaSrcPath -Recurse -Filter "*.java" -File -ErrorAction SilentlyContinue | Where-Object {
    $_.FullName -like "*\repository\*" -or $_.FullName -like "*\repositories\*" -or $_.FullName -like "*\dao\*"
}

foreach ($repo in $repositorios) {
    if ($repo.BaseName -notlike "*Repository" -and $repo.BaseName -notlike "*Repo" -and $repo.BaseName -notlike "*Dao") {
        Write-Host "  [WARN] '$($repo.Name)' esta en paquete repository pero no sigue convencion" -ForegroundColor Yellow
        $advertencias += "$($repo.Name) no sigue convencion"
    }
}

if ($controladores.Count -eq 0 -and $servicios.Count -eq 0 -and $repositorios.Count -eq 0) {
    Write-Host "  [INFO] No se encontraron archivos en paquetes estandar" -ForegroundColor Gray
}

Write-Host ""

# ============================================================================
# 4. VERIFICAR ANOTACIONES SPRING
# ============================================================================
Write-Host "4. Verificando anotaciones Spring..." -ForegroundColor Cyan
Write-Host ""

$anotacionesCorrectas = 0
$anotacionesIncorrectas = 0

foreach ($ctrl in $controladores) {
    $contenido = Get-Content -Path $ctrl.FullName -Raw -Encoding UTF8
    if ($contenido -match "@RestController|@Controller") {
        $anotacionesCorrectas++
    } else {
        Write-Host "  [ERROR] '$($ctrl.Name)' no tiene @RestController o @Controller" -ForegroundColor Red
        $errores += "$($ctrl.Name) sin anotacion de controlador"
        $anotacionesIncorrectas++
    }
}

foreach ($svc in $servicios) {
    $contenido = Get-Content -Path $svc.FullName -Raw -Encoding UTF8
    if ($contenido -match "@Service|@Component|@Transactional") {
        $anotacionesCorrectas++
    } else {
        Write-Host "  [WARN] '$($svc.Name)' no tiene @Service, @Component o @Transactional" -ForegroundColor Yellow
        $advertencias += "$($svc.Name) sin anotacion de servicio"
        $anotacionesIncorrectas++
    }
}

foreach ($repo in $repositorios) {
    $contenido = Get-Content -Path $repo.FullName -Raw -Encoding UTF8
    if ($contenido -match "extends JpaRepository|extends CrudRepository|extends PagingAndSortingRepository|extends MongoRepository|extends JpaRepository|@Repository") {
        $anotacionesCorrectas++
    } else {
        Write-Host "  [WARN] '$($repo.Name)' no extiende de repositorio Spring ni tiene @Repository" -ForegroundColor Yellow
        $advertencias += "$($repo.Name) sin anotacion de repositorio"
        $anotacionesIncorrectas++
    }
}

Write-Host "  Anotaciones correctas: $anotacionesCorrectas" -ForegroundColor Green
Write-Host "  Anotaciones incorrectas/faltantes: $anotacionesIncorrectas" -ForegroundColor $(if ($anotacionesIncorrectas -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# ============================================================================
# 5. VERIFICAR ARQUITECTURA EN CAPAS
# ============================================================================
Write-Host "5. Verificando arquitectura en capas..." -ForegroundColor Cyan
Write-Host ""

$violacionesArquitectura = 0

foreach ($ctrl in $controladores) {
    $contenido = Get-Content -Path $ctrl.FullName -Raw -Encoding UTF8
    if ($contenido -match "import.*\.repository\.|import.*\.dao\.") {
        Write-Host "  [ERROR] '$($ctrl.Name)' importa repositorios directamente (viola arquitectura)" -ForegroundColor Red
        $errores += "$($ctrl.Name) importa repositorios"
        $violacionesArquitectura++
    }
}

foreach ($svc in $servicios) {
    $contenido = Get-Content -Path $svc.FullName -Raw -Encoding UTF8
    if ($contenido -match "import.*\.controller\.") {
        Write-Host "  [ERROR] '$($svc.Name)' importa controladores (dependencia circular)" -ForegroundColor Red
        $errores += "$($svc.Name) importa controladores"
        $violacionesArquitectura++
    }
}

if ($violacionesArquitectura -eq 0) {
    Write-Host "  [OK] No hay violaciones de arquitectura en capas" -ForegroundColor Green
}
Write-Host ""

# ============================================================================
# 6. VERIFICAR CLASE PRINCIPAL
# ============================================================================
Write-Host "6. Verificando clase principal..." -ForegroundColor Cyan
Write-Host ""

if ($appClass) {
    Write-Host "  [OK] Clase principal encontrada: '$($appClass.Name)'" -ForegroundColor Green
    $contenido = Get-Content -Path $appClass.FullName -Raw -Encoding UTF8
    if ($contenido -match "@SpringBootApplication") {
        Write-Host "  [OK] Tiene anotacion @SpringBootApplication" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] No tiene anotacion @SpringBootApplication" -ForegroundColor Red
        $errores += "Clase principal sin @SpringBootApplication"
    }
} else {
    Write-Host "  [ERROR] No se encontro clase principal (*Application.java)" -ForegroundColor Red
    $errores += "Clase principal no encontrada"
}
Write-Host ""

# ============================================================================
# 7. VERIFICAR ARCHIVOS DE CONFIGURACION
# ============================================================================
Write-Host "7. Verificando archivos de configuracion..." -ForegroundColor Cyan
Write-Host ""

$configFiles = @(
    "src\main\resources\application.properties",
    "src\main\resources\application.yml",
    "src\main\resources\application.yaml",
    "pom.xml",
    "build.gradle"
)

$configEncontrados = 0
foreach ($config in $configFiles) {
    $ruta = Join-Path $projectPath $config
    if (Test-Path $ruta) {
        Write-Host "  [OK] $(Split-Path $config -Leaf) existe" -ForegroundColor Green
        $configEncontrados++
    }
}

if ($configEncontrados -eq 0) {
    Write-Host "  [ERROR] No se encontro ningun archivo de configuracion estandar" -ForegroundColor Red
    $errores += "Sin archivos de configuracion"
}
Write-Host ""

# ============================================================================
# 8. RESUMEN FINAL
# ============================================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  RESUMEN FINAL" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Correctos: $($correctos.Count)" -ForegroundColor Green
Write-Host "Advertencias: $($advertencias.Count)" -ForegroundColor Yellow
Write-Host "Errores: $($errores.Count)" -ForegroundColor Red
Write-Host ""

if ($errores.Count -gt 0) {
    Write-Host "--------------------------------------------------------" -ForegroundColor Red
    Write-Host "  ERRORES CRITICOS (deben corregirse):" -ForegroundColor Red
    Write-Host "--------------------------------------------------------" -ForegroundColor Red
    foreach ($error in $errores) {
        Write-Host "  [X] $error" -ForegroundColor Red
    }
    Write-Host ""
}

if ($advertencias.Count -gt 0) {
    Write-Host "--------------------------------------------------------" -ForegroundColor Yellow
    Write-Host "  ADVERTENCIAS (recomendado corregir):" -ForegroundColor Yellow
    Write-Host "--------------------------------------------------------" -ForegroundColor Yellow
    foreach ($adv in $advertencias) {
        Write-Host "  [!] $adv" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($errores.Count -eq 0) {
    Write-Host "??Excelente! Tu proyecto cumple con la arquitectura Spring Boot." -ForegroundColor Green
    Write-Host ""
} elseif ($errores.Count -le 3 -and $advertencias.Count -le 5) {
    Write-Host "Tu proyecto esta bien estructurado, pero tiene algunas advertencias." -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "Tu proyecto tiene problemas de arquitectura que deben corregirse." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "Presiona cualquier tecla para continuar..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
