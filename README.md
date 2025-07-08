# 현대차 의장공정 디지털 트윈 시스템

## 프로젝트 개요
현대차 의장공정의 실시간 모니터링 및 관리를 위한 디지털 트윈 시스템

## 시스템 구성
```
manufacturing_process/
├── dashboard_backend/     # Spring Boot API 서버 (포트: 8080)
├── dashboard_frontend/    # React 대시보드 (포트: 5173)
├── data_collector/        # Python 데이터 수집 서버 (포트: 8082)
├── mosquitto_MQTT/        # MQTT 시뮬레이터 (15개 스테이션)
├── venv/                  # Python 가상환경
├── start_macos.sh         # 🍎 macOS 자동 실행 스크립트
└── start_windows.bat      # 🪟 Windows 자동 실행 스크립트
```

## 기술 스택
- **Backend**: Spring Boot 3.1.5, Java 17, Gradle
- **Frontend**: React 18, Vite, Tabler.io
- **Data Collector**: Python 3.8+, MQTT, InfluxDB
- **Database**: PostgreSQL (메인), InfluxDB 3.x (시계열)
- **MQTT Broker**: Mosquitto 2.0.21

## 사전 설치
- **Node.js**: v22.16.0 - https://nodejs.org/
- **Java**: 17+ 
- **Python**: 3.8+
- **Mosquitto**: 
  - macOS: `brew install mosquitto`
  - Windows: `choco install mosquitto`

## 초기 설정 (한 번만)
```bash
# Python 가상환경 생성
python3 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
cd mosquitto_MQTT && pip install -r requirements.txt
cd ../data_collector && pip install -r requirements.txt
cd ../dashboard_frontend && npm install
```

## 🚀 간편 실행 (자동화 스크립트)

### macOS 사용자
```bash
# 1. 가상환경 활성화
source venv/bin/activate

# 2. 스크립트 실행
./start_macos.sh
```

### Windows 사용자
#### 중요!!!!==================  C:\Program Files\mosquitto\mosquitto.exe 이 부분을 mosquitto가 설치되어있는 폴더로 바꾸기!!!!!!
powershell 로 열기.
.\start.ps1
만약 한글이 깨질시 
chcp 65001 입력후 커맨드 실행

안 되면 
$env:LC_ALL='C.UTF-8'
[System.Console]::InputEncoding = [System.Console]::OutputEncoding = [System.Text.Encoding]::UTF8
입력후 실행

안 되면 
vscode 설정 or (컨트롤 + ,) 들어가서 밑의 커멘드 입력.
"files.encoding": "utf8bom"
utf8 Bom 으로 설정하면 한글 해결

**자동화 스크립트 기능:**
- ✅ 사전 요구사항 자동 확인 (Node.js, Java, Python, Mosquitto)
- ✅ 포트 사용 상태 체크 (8080, 5173, 1883)
- ✅ 모든 서비스 순차 시작 및 상태 확인
- ✅ 로그 파일 자동 생성 (`logs/` 디렉토리)
- ✅ 웹 브라우저 자동 열기 옵션
- ✅ Ctrl+C로 모든 서비스 안전 종료

---

## 📋 수동 실행 (개발자용)

### 1. 가상환경 활성화
```bash
source venv/bin/activate  # Windows: venv\Scripts\activate
```

### 2. Mosquitto 브로커 시작 (터미널 1)
```bash
# macOS
mosquitto -c /opt/homebrew/etc/mosquitto/mosquitto.conf

# Windows
mosquitto
```

### 3. Spring Boot 백엔드 (터미널 2)
```bash
cd dashboard_backend
./gradlew bootRun  # Windows: gradlew.bat bootRun
```

### 4. 데이터 수집기 (터미널 3)
```bash
cd data_collector
python main.py
```

### 5. MQTT 시뮬레이터 (터미널 4)
```bash
cd mosquitto_MQTT
python run_simulation.py
```

### 6. React 프론트엔드 (터미널 5)
```bash
cd dashboard_frontend
npm run dev
```

## 접속 주소
- **대시보드**: http://localhost:5173
- **백엔드 API**: http://localhost:8080/api/kpi/factory/summary

## 종료

### 자동화 스크립트 사용 시
- `Ctrl+C` (macOS) 또는 아무키 (Windows)
- 모든 서비스가 자동으로 안전 종료됩니다

### 수동 실행 시  
- 각 터미널에서 `Ctrl+C`
- 가상환경 비활성화: `deactivate`

## 15개 공정 스테이션
**A라인**: A01_DOOR → A02_WIRING → A03_HEADLINER → A04_CRASH_PAD  
**B라인**: B01_FUEL_TANK → B02_CHASSIS_MERGE → B03_MUFFLER  
**C라인**: C01_FEM → C02_GLASS → C03_SEAT → C04_BUMPER → C05_TIRE  
**D라인**: D01_WHEEL_ALIGNMENT → D02_HEADLAMP → D03_WATER_LEAK_TEST


konva 설치
npm install konva react-konva