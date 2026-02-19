
# K6 테스트 실행 스크립트

param (
    [string]$Type = "all" # all, grpc, rest
)

$ResultDir = "results"
if (-not (Test-Path $ResultDir)) {
    New-Item -ItemType Directory -Path $ResultDir | Out-Null
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

function Run-K6Test {
    param ($Script, $Name)
    Write-Host "Running $Name Benchmark..." -ForegroundColor Cyan
    $OutputFile = "$ResultDir/result_${Name}_${Timestamp}.json"
    
    # K6 명령어 확인 및 경로 설정
    $K6Cmd = "k6"
    if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
        if (Test-Path "C:\Program Files\k6\k6.exe") {
            $K6Cmd = "& 'C:\Program Files\k6\k6.exe'"
            Write-Host "K6 not found in PATH, using default path: C:\Program Files\k6\k6.exe" -ForegroundColor Yellow
        }
        else {
            Write-Error "K6를 찾을 수 없습니다. 설치가 되었는지 확인하거나 터미널을 재시작해주세요."
            return
        }
    }

    # JSON 결과 저장 및 콘솔 출력
    Invoke-Expression "$K6Cmd run $Script --summary-export=$OutputFile"
    
    Write-Host "Test finished. Result saved to $OutputFile" -ForegroundColor Green
}

if ($Type -eq "all" -or $Type -eq "grpc") {
    Run-K6Test -Script "chat_grpc_benchmark.js" -Name "grpc"
}

if ($Type -eq "all" -or $Type -eq "rest") {
    Run-K6Test -Script "chat_rest_benchmark.js" -Name "rest"
}

if ($Type -eq "all" -or $Type -eq "extension") {
    Run-K6Test -Script "extension_sync_benchmark.js" -Name "extension"
}

if ($Type -eq "all" -or $Type -eq "polling") {
    Run-K6Test -Script "extension_polling_benchmark.js" -Name "polling"
}
