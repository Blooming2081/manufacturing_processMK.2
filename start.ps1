# 2D Twin 시스템 시작 - PowerShell 버전
# UTF-8 인코딩 설정
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Write-Host "🏭 2D Twin 시스템 시작" -ForegroundColor Green
Write-Host "==================" -ForegroundColor Green

# 현재 디렉토리 저장
$rootPath = Get-Location

Write-Host "`n1️⃣ Spring Boot 백엔드 시작 중..." -ForegroundColor Yellow
Start-Job -Name "Backend" -ScriptBlock { 
    param($path)
    Set-Location "$path\dashboard_backend"
    .\gradlew.bat bootRun 
} -ArgumentList $rootPath

Write-Host "2️⃣ Mosquitto MQTT 브로커 시작 중..." -ForegroundColor Yellow  
Start-Job -Name "Mosquitto" -ScriptBlock { 
    & "C:\Program Files\mosquitto\mosquitto.exe" -v 
}

Write-Host "3️⃣ 데이터 수집기 시작 중..." -ForegroundColor Yellow
Start-Job -Name "DataCollector" -ScriptBlock { 
    param($path)
    Set-Location "$path\data_collector"
    python main.py 
} -ArgumentList $rootPath

Write-Host "4️⃣ MQTT 시뮬레이터 시작 중..." -ForegroundColor Yellow
Start-Job -Name "MQTTSimulator" -ScriptBlock { 
    param($path)
    Set-Location "$path\mosquitto_MQTT"
    python run_simulation.py 
} -ArgumentList $rootPath

Write-Host "5️⃣ React 프론트엔드 시작 중..." -ForegroundColor Yellow
Start-Job -Name "Frontend" -ScriptBlock { 
    param($path)
    Set-Location "$path\dashboard_frontend"
    npm run dev 
} -ArgumentList $rootPath

Write-Host "`n⏳ 서비스 시작 대기 중..." -ForegroundColor Cyan
Start-Sleep -Seconds 10

# 작업 상태 확인
Write-Host "`n📊 실행 중인 서비스:" -ForegroundColor Green
Get-Job | Format-Table Name, State, HasMoreData

Write-Host "`n🌐 접속 주소:" -ForegroundColor Green
Write-Host "  • 대시보드: http://localhost:5173" -ForegroundColor White
Write-Host "  • 백엔드 API: http://localhost:8080" -ForegroundColor White

Write-Host "`n📝 유용한 명령어:" -ForegroundColor Green
Write-Host "  • 상태 확인: Get-Job" -ForegroundColor White
Write-Host "  • 로그 보기: Receive-Job -Name 'Backend'" -ForegroundColor White
Write-Host "  • 서비스 종료: Stop-Job -Name 'Backend'" -ForegroundColor White
Write-Host "  • 모든 종료: Get-Job | Stop-Job; Get-Job | Remove-Job" -ForegroundColor White

Write-Host "`n🛑 종료하려면 아무 키나 누르세요..." -ForegroundColor Red
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

Write-Host "`n🛑 모든 서비스 종료 중..." -ForegroundColor Red
Get-Job | Stop-Job
Get-Job | Remove-Job
Write-Host "✓ 모든 프로세스 종료 완료" -ForegroundColor Green