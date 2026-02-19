
# K6 설치 스크립트

Write-Host "K6 설치를 시작합니다..." -ForegroundColor Cyan

# 1. Chocolatey 확인 및 설치 시도
if (Get-Command choco -ErrorAction SilentlyContinue) {
    Write-Host "Chocolatey가 감지되었습니다. Chocolatey로 설치를 시도합니다." -ForegroundColor Green
    choco install k6 -y
} 
# 2. Winget 확인 및 설치 시도
elseif (Get-Command winget -ErrorAction SilentlyContinue) {
    Write-Host "Winget이 감지되었습니다. Winget으로 설치를 시도합니다." -ForegroundColor Green
    winget install k6
} 
else {
    Write-Host "패키지 관리자(Chocolatey 또는 Winget)를 찾을 수 없습니다." -ForegroundColor Red
    Write-Host "아래 링크에서 수동으로 설치해주세요:"
    Write-Host "https://k6.io/docs/get-started/installation/"
    exit 1
}

# 3. 설치 확인
$k6Path = Get-Command k6 -ErrorAction SilentlyContinue
if ($k6Path) {
    Write-Host "K6 설치가 완료되었습니다!" -ForegroundColor Green
    k6 version
} else {
    Write-Host "설치 후 경로 설정이 필요할 수 있습니다. 터미널을 재시작해주세요." -ForegroundColor Yellow
}
