# PowerShell runner for Examination Timetabling Platform
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Building and Launching Academic Examination Timetabling Platform" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "Compiling Java source files..." -ForegroundColor Yellow
$sources = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -d bin -sourcepath src\main\java $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Java compilation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Compilation successful. Launching HTTP server on http://localhost:8080 ..." -ForegroundColor Green
& java -cp bin edu.ump.timetabling.Main 8080
