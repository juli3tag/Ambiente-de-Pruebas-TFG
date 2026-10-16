param(
    [string]$OutputFile = ".\monitoring\resources.csv"
)

$interval = 2

$header = "Timestamp,Host_CPU_Pct,Host_RAM_Pct,Host_RAM_Used_MB,Host_RAM_Available_MB,Postgres_CPU_Pct,Postgres_RAM_Used_MB,Postgres_RAM_Limit_MB,Postgres_RAM_Pct,Java_RAM_MB"

$header | Out-File -FilePath $OutputFile -Encoding utf8

Write-Host "Monitoreo iniciado."
Write-Host "Archivo: $OutputFile"
Write-Host "Presione Ctrl+C para detener."

while ($true) {

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"

    # CPU del equipo
    $cpu = (Get-CimInstance Win32_Processor |
        Measure-Object -Property LoadPercentage -Average).Average

    # RAM del equipo
    $os = Get-CimInstance Win32_OperatingSystem

    $totalRAM = $os.TotalVisibleMemorySize / 1MB
    $freeRAM = $os.FreePhysicalMemory / 1MB
    $usedRAM = $totalRAM - $freeRAM
    $ramPct = ($usedRAM / $totalRAM) * 100

    # PostgreSQL - Docker
    $dockerStats = docker stats tfg_postgres --no-stream --format "{{.CPUPerc}},{{.MemUsage}}"

    $postgresCPU = 0
    $postgresUsed = 0
    $postgresLimit = 0
    $postgresPct = 0

    if ($dockerStats) {

        $parts = $dockerStats -split ","

        $postgresCPU = [double]($parts[0] -replace "%","")

        $memoryParts = $parts[1] -split "/"

        $postgresUsed = [double]($memoryParts[0] -replace "[^\d\.]","")
        $postgresLimit = [double]($memoryParts[1] -replace "[^\d\.]","")

        $postgresPct = ($postgresUsed / $postgresLimit) * 100
    }

    # Java
    $java = Get-Process java -ErrorAction SilentlyContinue

    $javaRAM = 0

    if ($java) {
        $javaRAM = ($java |
            Measure-Object WorkingSet64 -Sum).Sum / 1MB
    }

    $line = "{0},{1:N2},{2:N2},{3:N2},{4:N2},{5:N2},{6:N2},{7:N2},{8:N2},{9:N2}" -f `
        $timestamp,
        $cpu,
        $ramPct,
        $usedRAM,
        $freeRAM,
        $postgresCPU,
        $postgresUsed,
        $postgresLimit,
        $postgresPct,
        $javaRAM

    Add-Content -Path $OutputFile -Value $line

    Start-Sleep -Seconds $interval
}