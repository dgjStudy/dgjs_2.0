# DGJS 2.0 개발 서버 실행 스크립트 (Windows PowerShell 5.1 이상)
# 사용법: .\dev.ps1 [-Backend] [-Frontend] [-Stop] [-Window] [-NoWt]
#   옵션 없음 : 백엔드와 프론트를 별도 창 없이 백그라운드로 실행하고 로그는 .dev-logs\ 에 기록
#   -Backend / -Frontend : 한쪽만 실행 (-Stop과 함께 쓰면 한쪽만 종료)
#   -Stop : 8080, 5173 포트를 사용 중인 java/node 프로세스를 종료
#   -Window : 별도 창(Windows Terminal 탭)에 실행해 로그를 직접 봄
#   -NoWt : -Window와 함께 쓰며 Windows Terminal 대신 PowerShell 창을 각각 띄움

param(
    [switch]$Backend,
    [switch]$Frontend,
    [switch]$Stop,
    [switch]$Window,
    [switch]$NoWt,
    [string]$JdkRoot = (Join-Path $HOME '.jdks'),
    [string]$MysqlService = 'MYSQL*'
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$backendDir = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'frontend'
$backendPort = 8080
$frontendPort = 5173

# 선택 옵션이 없으면 둘 다 대상임
if (-not $Backend -and -not $Frontend) {
    $Backend = $true
    $Frontend = $true
}

function Fail([string]$Message) {
    Write-Host "[오류] $Message" -ForegroundColor Red
    exit 1
}

function Get-ListenPids([int]$Port) {
    @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)
}

function Assert-PortFree([string]$Name, [int]$Port) {
    $ids = Get-ListenPids $Port
    if ($ids.Count -gt 0) {
        $proc = Get-Process -Id $ids[0] -ErrorAction SilentlyContinue
        Fail "$Name 포트($Port)를 이미 사용 중입니다 ($($proc.ProcessName), PID $($ids[0])). '.\dev.ps1 -Stop' 으로 종료하거나 포트를 사용하는 프로그램을 확인하세요."
    }
}

if ($Stop) {
    $targets = @()
    if ($Backend) { $targets += @{ Name = '백엔드'; Port = $backendPort } }
    if ($Frontend) { $targets += @{ Name = '프론트'; Port = $frontendPort } }
    foreach ($target in $targets) {
        $ids = Get-ListenPids $target.Port
        if ($ids.Count -eq 0) {
            Write-Host "$($target.Name) ($($target.Port)): 실행 중인 프로세스 없음"
            continue
        }
        foreach ($procId in $ids) {
            $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
            if (-not $proc) { continue }
            # 다른 프로그램을 실수로 종료하지 않도록 java, node만 대상으로 함
            if ($proc.ProcessName -notin @('java', 'javaw', 'node')) {
                Write-Host "$($target.Name) ($($target.Port)): $($proc.ProcessName)(PID $procId)이 사용 중이라 종료하지 않음" -ForegroundColor Yellow
                continue
            }
            Stop-Process -Id $procId -Force
            Write-Host "$($target.Name) ($($target.Port)): $($proc.ProcessName)(PID $procId) 종료"
        }
    }
    exit 0
}

$javaHome = $null
if ($Backend) {
    $mysql = @(Get-Service -Name $MysqlService -ErrorAction SilentlyContinue)
    if ($mysql.Count -eq 0) {
        Write-Host "[주의] MySQL 서비스($MysqlService)를 찾지 못했습니다. 외부 DB를 쓰는 경우가 아니라면 MySQL 설치와 실행 상태를 확인하세요." -ForegroundColor Yellow
    } elseif ($mysql[0].Status -ne 'Running') {
        Fail "MySQL 서비스($($mysql[0].Name))가 '$($mysql[0].Status)' 상태입니다. 관리자 권한 PowerShell에서 'Start-Service $($mysql[0].Name)' 으로 시작한 뒤 다시 실행하세요."
    }

    $javaHome = $env:JAVA_HOME
    if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
        $jdk = $null
        if (Test-Path $JdkRoot) {
            $jdk = Get-ChildItem -Path $JdkRoot -Directory |
                Where-Object { $_.Name -match '(^|[-_])17([._]|$)' } |
                Sort-Object Name -Descending |
                Select-Object -First 1
        }
        if (-not $jdk) {
            Fail "JAVA_HOME이 없고 $JdkRoot 에서 JDK 17도 찾지 못했습니다. JDK 17을 설치하거나 JAVA_HOME을 지정하세요."
        }
        $javaHome = $jdk.FullName
        Write-Host "JAVA_HOME 미설정 -> $javaHome 사용"
    }

    Assert-PortFree '백엔드' $backendPort
}

if ($Frontend) {
    if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
        Fail 'npm을 찾지 못했습니다. Node.js를 설치하세요.'
    }
    if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
        Fail "frontend\node_modules 가 없습니다. 'cd frontend; npm install' 을 먼저 실행하세요."
    }
    Assert-PortFree '프론트' $frontendPort
}

# 따옴표와 세미콜론 처리 문제를 피하기 위해 명령을 인코딩해서 전달함
function ConvertTo-EncodedCommand([string]$Command) {
    [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($Command))
}

# 일부 터미널은 ComSpec(cmd.exe 경로) 환경변수가 없어 npm run이 조용히 종료되므로 새 창마다 보정함
$comspecFix = 'if (-not $env:ComSpec) { $env:ComSpec = "$env:SystemRoot\System32\cmd.exe" }'

$servers = @()
if ($Backend) {
    $servers += @{
        Title = 'DGJS backend'; Log = 'backend.log'
        Body = "`$env:JAVA_HOME = '$javaHome'; Set-Location '$backendDir'; .\gradlew.bat bootRun"
    }
}
if ($Frontend) {
    $servers += @{
        Title = 'DGJS frontend'; Log = 'frontend.log'
        Body = "[Console]::OutputEncoding = [Text.Encoding]::UTF8; Set-Location '$frontendDir'; npm run dev"
    }
}

$logDir = Join-Path $root '.dev-logs'
$procs = @()
if ($Window) {
    $tabs = @($servers | ForEach-Object {
        ConvertTo-EncodedCommand "`$Host.UI.RawUI.WindowTitle = '$($_.Title)'; $comspecFix; $($_.Body)"
    })
    $wt = $null
    if (-not $NoWt) { $wt = Get-Command wt -ErrorAction SilentlyContinue }
    if ($wt) {
        $tabArgs = $tabs | ForEach-Object { "new-tab powershell -NoExit -EncodedCommand $_" }
        Start-Process -FilePath $wt.Source -ArgumentList ('-w new ' + ($tabArgs -join ' ; '))
    } else {
        foreach ($encoded in $tabs) {
            Start-Process -FilePath 'powershell' -ArgumentList '-NoExit', '-EncodedCommand', $encoded
        }
    }
} else {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
    foreach ($server in $servers) {
        $logPath = Join-Path $logDir $server.Log
        Set-Content -Path $logPath -Value '' -Encoding UTF8
        # node는 UTF-8로 출력하므로 프론트 본문에서 콘솔 출력 인코딩을 맞춤 (Java 17은 기본 인코딩 그대로 둠)
        # 표준 오류도 한 줄씩 문자열로 바꿔 기록함 (PowerShell 5.1이 오류 객체 형식으로 감싸는 것을 방지)
        $writer = 'ForEach-Object { $t = if ($_ -is [System.Management.Automation.ErrorRecord]) { $_.Exception.Message } else { [string]$_ }; $t | Out-File -FilePath ''LOGPATH'' -Append -Encoding UTF8 }'
        $command = "$comspecFix; $($server.Body) 2>&1 | " + $writer.Replace('LOGPATH', $logPath)
        $procs += Start-Process -FilePath 'powershell' -WindowStyle Hidden -PassThru `
            -ArgumentList '-NoProfile', '-EncodedCommand', (ConvertTo-EncodedCommand $command)
    }
}

Write-Host '서버를 시작했습니다. 준비될 때까지 기다립니다 (최대 120초)...'
$pending = @{}
if ($Backend) { $pending['백엔드 http://localhost:8080'] = $backendPort }
if ($Frontend) { $pending['프론트 http://localhost:5173'] = $frontendPort }
$deadline = (Get-Date).AddSeconds(120)
while ($pending.Count -gt 0 -and (Get-Date) -lt $deadline) {
    foreach ($key in @($pending.Keys)) {
        if ((Get-ListenPids $pending[$key]).Count -gt 0) {
            Write-Host "준비됨: $key" -ForegroundColor Green
            $pending.Remove($key)
        }
    }
    if ($pending.Count -gt 0 -and @($procs | Where-Object { $_.HasExited }).Count -gt 0) {
        break
    }
    if ($pending.Count -gt 0) { Start-Sleep -Seconds 2 }
}
if ($pending.Count -gt 0) {
    if ($Window) {
        Write-Host "[주의] 시간 안에 준비되지 않았습니다: $($pending.Keys -join ', '). 각 창의 로그를 확인하세요." -ForegroundColor Yellow
    } else {
        Write-Host "[주의] 준비되지 않았거나 도중에 종료되었습니다: $($pending.Keys -join ', ')" -ForegroundColor Yellow
        foreach ($server in $servers) {
            $logPath = Join-Path $logDir $server.Log
            Write-Host "--- $($server.Log) 마지막 20줄 ---"
            Get-Content -Path $logPath -Tail 20 -ErrorAction SilentlyContinue
        }
        Write-Host "전체 로그: $logDir"
    }
} else {
    if (-not $Window) {
        Write-Host "로그: $logDir (실시간 보기: Get-Content '$logDir\backend.log' -Wait)"
    }
    Write-Host '종료하려면 .\dev.ps1 -Stop 을 실행하세요.'
}
