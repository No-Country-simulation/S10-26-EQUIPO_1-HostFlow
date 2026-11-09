Write-Host "Iniciando..." -ForegroundColor Yellow

$outputFile = "C:\Users\carlo\todo_el_proyecto.txt"

if (Test-Path $outputFile) { Remove-Item $outputFile }

Write-Host "Buscando archivos Java..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path "C:\Users\carlo\NO_COUNTRY-HostFlow\S10-26-EQUIPO_1-HostFlow\backend\src" -Recurse -Filter "*.java"
Write-Host "  Encontrados: $($javaFiles.Count)" -ForegroundColor Green

Write-Host "Buscando archivos properties..." -ForegroundColor Cyan
$propFiles = Get-ChildItem -Path "C:\Users\carlo\NO_COUNTRY-HostFlow\S10-26-EQUIPO_1-HostFlow\backend\src" -Recurse -Filter "*.properties"
Write-Host "  Encontrados: $($propFiles.Count)" -ForegroundColor Green

Write-Host "Buscando archivos SQL..." -ForegroundColor Cyan
$sqlFiles = Get-ChildItem -Path "C:\Users\carlo\NO_COUNTRY-HostFlow\S10-26-EQUIPO_1-HostFlow\backend\src" -Recurse -Filter "*.sql"
Write-Host "  Encontrados: $($sqlFiles.Count)" -ForegroundColor Green

Write-Host "Buscando archivos HTML..." -ForegroundColor Cyan
$htmlFiles = Get-ChildItem -Path "C:\Users\carlo\NO_COUNTRY-HostFlow\S10-26-EQUIPO_1-HostFlow\backend\src" -Recurse -Filter "*.html"
Write-Host "  Encontrados: $($htmlFiles.Count)" -ForegroundColor Green

$allFiles = $javaFiles + $propFiles + $sqlFiles + $htmlFiles

Write-Host "Total de archivos: $($allFiles.Count)" -ForegroundColor Magenta

foreach ($file in $allFiles) {
    Add-Content -Path $outputFile -Value ""
    Add-Content -Path $outputFile -Value ("#" * 80)
    Add-Content -Path $outputFile -Value "ARCHIVO: $($file.Name)"
    Add-Content -Path $outputFile -Value "RUTA: $($file.FullName)"
    Add-Content -Path $outputFile -Value ("#" * 80)
    Add-Content -Path $outputFile -Value ""
    Get-Content -Path $file.FullName -Encoding UTF8 | Add-Content -Path $outputFile
    Add-Content -Path $outputFile -Value ""
}

# Agregar pom.xml
$pomFile = "C:\Users\carlo\NO_COUNTRY-HostFlow\S10-26-EQUIPO_1-HostFlow\backend\pom.xml"
if (Test-Path $pomFile) {
    Add-Content -Path $outputFile -Value ""
    Add-Content -Path $outputFile -Value ("#" * 80)
    Add-Content -Path $outputFile -Value "ARCHIVO: pom.xml"
    Add-Content -Path $outputFile -Value ("#" * 80)
    Add-Content -Path $outputFile -Value ""
    Get-Content -Path $pomFile -Encoding UTF8 | Add-Content -Path $outputFile
}

# Agregar Dockerfile
$dockFile = "C:\Users\carlo\NO_COUNTRY-HostFlow\S10-26-EQUIPO_1-HostFlow\backend\Dockerfile"
if (Test-Path $dockFile) {
    Add-Content -Path $outputFile -Value ""
    Add-Content -Path $outputFile -Value ("#" * 80)
    Add-Content -Path $outputFile -Value "ARCHIVO: Dockerfile"
    Add-Content -Path $outputFile -Value ("#" * 80)
    Add-Content -Path $outputFile -Value ""
    Get-Content -Path $dockFile -Encoding UTF8 | Add-Content -Path $outputFile
}

Write-Host ""
Write-Host "LISTO! Archivo generado en:" -ForegroundColor Green
Write-Host "  $outputFile" -ForegroundColor White
Write-Host ""
Write-Host "Tamano del archivo: $((Get-Item $outputFile).Length) bytes" -ForegroundColor Cyan