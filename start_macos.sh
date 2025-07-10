#!/bin/bash

# 현대차 의장공정 디지털 트윈 시스템 - macOS 실행 스크립트
# 작성자: Claude Code
# 날짜: 2025-07-03

echo "🏭 현대차 의장공정 디지털 트윈 시스템 - macOS"
echo "============================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 전역 변수
MOSQUITTO_PID=""
BACKEND_PID=""
DATA_COLLECTOR_PID=""
MQTT_SIMULATOR_PID=""
FRONTEND_PID=""

# 에러 처리 함수
handle_error() {
    echo -e "${RED}❌ 오류 발생: $1${NC}"
    echo "시스템을 종료합니다..."
    cleanup
    exit 1
}

# 정리 함수
cleanup() {
    echo -e "\n${YELLOW}🛑 시스템 정리 중...${NC}"
    
    # 강제 종료: SIGTERM -> SIGKILL 순서로 시도
    echo "프로세스 종료 중..."
    
    # PID로 개별 종료 시도
    if [ ! -z "$MOSQUITTO_PID" ]; then
        kill -TERM $MOSQUITTO_PID 2>/dev/null
        sleep 1
        kill -KILL $MOSQUITTO_PID 2>/dev/null
        echo "✓ Mosquitto 브로커 종료"
    fi
    
    if [ ! -z "$BACKEND_PID" ]; then
        kill -TERM $BACKEND_PID 2>/dev/null
        sleep 2
        kill -KILL $BACKEND_PID 2>/dev/null
        echo "✓ Spring Boot 백엔드 종료"
    fi
    
    if [ ! -z "$DATA_COLLECTOR_PID" ]; then
        kill -TERM $DATA_COLLECTOR_PID 2>/dev/null
        sleep 1
        kill -KILL $DATA_COLLECTOR_PID 2>/dev/null
        echo "✓ 데이터 수집기 종료"
    fi
    
    if [ ! -z "$MQTT_SIMULATOR_PID" ]; then
        kill -TERM $MQTT_SIMULATOR_PID 2>/dev/null
        sleep 1
        kill -KILL $MQTT_SIMULATOR_PID 2>/dev/null
        echo "✓ MQTT 시뮬레이터 종료"
    fi
    
    if [ ! -z "$FRONTEND_PID" ]; then
        kill -TERM $FRONTEND_PID 2>/dev/null
        sleep 1
        kill -KILL $FRONTEND_PID 2>/dev/null
        echo "✓ React 프론트엔드 종료"
    fi
    
    # 강력한 프로세스 정리 (이름 기반)
    echo "남은 프로세스 강제 정리 중..."
    pkill -f "gradlew bootRun" 2>/dev/null
    pkill -f "GradleDaemon" 2>/dev/null
    pkill -f "mosquitto" 2>/dev/null
    pkill -f "run_simulation.py" 2>/dev/null
    pkill -f "main.py" 2>/dev/null
    pkill -f "npm run dev" 2>/dev/null
    pkill -f "vite" 2>/dev/null
    
    # Java 프로세스 특별 처리
    for java_pid in $(pgrep -f "bootRun"); do
        kill -KILL $java_pid 2>/dev/null
    done
    
    # Node.js 프로세스 특별 처리
    for node_pid in $(pgrep -f "vite.*5173"); do
        kill -KILL $node_pid 2>/dev/null
    done
    
    echo -e "${GREEN}정리 완료${NC}"
    echo -e "${YELLOW}💡 가상환경은 터미널을 종료하거나 'deactivate' 명령어로 비활성화하세요${NC}"
    exit 0
}

# 시그널 핸들러 등록
trap cleanup SIGINT SIGTERM

# 현재 디렉토리 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
mkdir -p logs

echo -e "${BLUE}📍 작업 디렉토리: $SCRIPT_DIR${NC}"

# 사전 요구사항 확인
echo -e "\n${YELLOW}🔍 사전 요구사항 확인${NC}"

# Node.js 확인
if ! command -v node >/dev/null 2>&1; then
    handle_error "Node.js가 설치되지 않았습니다. 'brew install node' 또는 https://nodejs.org"
else
    echo "✅ Node.js $(node --version)"
fi

# Java 확인
if ! command -v java >/dev/null 2>&1; then
    handle_error "Java가 설치되지 않았습니다. 'brew install openjdk' 또는 Oracle JDK"
else
    echo "✅ Java 설치됨"
fi

# Python 확인
if ! command -v python3 >/dev/null 2>&1; then
    handle_error "Python3가 설치되지 않았습니다. 'brew install python'"
else
    echo "✅ Python $(python3 --version)"
fi

# Mosquitto 확인
if ! command -v mosquitto >/dev/null 2>&1; then
    handle_error "Mosquitto가 설치되지 않았습니다. 'brew install mosquitto'"
else
    echo "✅ Mosquitto 설치됨"
fi

# 포트 사용 확인
echo -e "\n${YELLOW}🔌 포트 사용 상태 확인${NC}"
occupied_ports=()

if lsof -i :8080 >/dev/null 2>&1; then occupied_ports+=(8080); fi
if lsof -i :5173 >/dev/null 2>&1; then occupied_ports+=(5173); fi
if lsof -i :1883 >/dev/null 2>&1; then occupied_ports+=(1883); fi

if [[ ${#occupied_ports[@]} -gt 0 ]]; then
    echo -e "${YELLOW}⚠️  다음 포트가 사용 중입니다: ${occupied_ports[*]}${NC}"
    echo "계속 진행하시겠습니까? (y/n)"
    read -r response
    if [[ "$response" != "y" && "$response" != "Y" ]]; then
        exit 1
    fi
else
    echo "✅ 모든 포트(8080, 5173, 1883) 사용 가능"
fi

# 1. Python 가상환경 확인
echo -e "\n${YELLOW}1️⃣ Python 가상환경 확인${NC}"
if [[ -f "venv/bin/activate" ]]; then
    echo -e "${GREEN}✅ 가상환경 확인 완료${NC}"
    echo -e "${BLUE}💡 스크립트 실행 전에 가상환경을 활성화해주세요: source venv/bin/activate${NC}"
    
    # 가상환경 활성화 확인
    if [[ "$VIRTUAL_ENV" == "" ]]; then
        echo -e "${RED}❌ 가상환경이 활성화되지 않았습니다${NC}"
        echo -e "${YELLOW}다음 명령어를 실행한 후 다시 시도하세요:${NC}"
        echo "source venv/bin/activate"
        echo "./start_macos.sh"
        exit 1
    else
        echo -e "${GREEN}✅ 가상환경 활성화 상태 확인${NC}"
    fi
else
    handle_error "가상환경이 없습니다. 'python3 -m venv venv'로 생성하세요."
fi

# 2. Mosquitto 브로커 시작
echo -e "\n${YELLOW}2️⃣ Mosquitto MQTT 브로커 시작${NC}"
if [[ -f "/opt/homebrew/etc/mosquitto/mosquitto.conf" ]]; then
    mosquitto -c /opt/homebrew/etc/mosquitto/mosquitto.conf >logs/mosquitto.log 2>&1 &
elif [[ -f "/usr/local/etc/mosquitto/mosquitto.conf" ]]; then
    mosquitto -c /usr/local/etc/mosquitto/mosquitto.conf >logs/mosquitto.log 2>&1 &
else
    mosquitto >logs/mosquitto.log 2>&1 &
fi

MOSQUITTO_PID=$!
echo -e "${GREEN}✅ Mosquitto 브로커 시작 완료 (PID: $MOSQUITTO_PID)${NC}"
sleep 3

# 3. 데이터베이스 연결 확인
echo -e "\n${YELLOW}3️⃣ 데이터베이스 연결 확인${NC}"

echo "PostgreSQL 연결 상태 확인 중... (proxy151.r3proxy.com:34209)"
if timeout 5 nc -z proxy151.r3proxy.com 34209 2>/dev/null; then
    echo -e "${GREEN}✅ PostgreSQL 연결 성공${NC}"
else
    echo -e "\n${RED}❌ 데이터베이스 연결 실패${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}🔌 PostgreSQL 서버에 연결할 수 없습니다${NC}"
    echo ""
    echo -e "${BLUE}📋 확인 사항:${NC}"
    echo "  1. remote.it 대시보드에서 PostgreSQL 서비스 상태 확인"
    echo "     → https://app.remote.it"
    echo ""
    echo "  2. 데이터베이스 엔드포인트 확인:"
    echo "     • 호스트: proxy151.r3proxy.com"
    echo "     • 포트: 34209"
    echo "     • 데이터베이스: manufacturing_dashboard"
    echo ""
    echo "  3. remote.it에서 서비스가 'Connected' 상태인지 확인"
    echo "  4. 네트워크 연결 상태 확인"
    echo ""
    echo -e "${YELLOW}📞 조치 방법:${NC}"
    echo "  • remote.it 포털에서 PostgreSQL 서비스 재시작"
    echo "  • 서비스가 온라인 상태가 된 후 다시 실행"
    echo ""
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    cleanup
    exit 1
fi

echo "InfluxDB 연결 상태 확인 중... (proxy151.r3proxy.com:34200)"
if timeout 5 nc -z proxy151.r3proxy.com 34200 2>/dev/null; then
    echo -e "${GREEN}✅ InfluxDB 연결 성공${NC}"
else
    echo -e "\n${RED}❌ InfluxDB 연결 실패${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}🔌 InfluxDB 서버에 연결할 수 없습니다${NC}"
    echo ""
    echo -e "${BLUE}📋 확인 사항:${NC}"
    echo "  1. remote.it 대시보드에서 InfluxDB 서비스 상태 확인"
    echo "     → https://app.remote.it"
    echo ""
    echo "  2. 데이터베이스 엔드포인트 확인:"
    echo "     • 호스트: proxy151.r3proxy.com"
    echo "     • 포트: 34200"
    echo "     • 데이터베이스: IOT-sensor"
    echo ""
    echo -e "${YELLOW}📞 조치 방법:${NC}"
    echo "  • remote.it 포털에서 InfluxDB 서비스 재시작"
    echo "  • 서비스가 온라인 상태가 된 후 다시 실행"
    echo ""
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    cleanup
    exit 1
fi

# 4. Spring Boot 백엔드 시작
echo -e "\n${YELLOW}4️⃣ Spring Boot 백엔드 시작${NC}"
echo "백엔드 컴파일 및 시작 중... (약 30초 소요)"

cd dashboard_backend
chmod +x gradlew
./gradlew bootRun >../logs/backend.log 2>&1 &
BACKEND_PID=$!
cd ..

echo -e "${GREEN}✅ Spring Boot 백엔드 시작 완료 (PID: $BACKEND_PID)${NC}"

# 백엔드 준비 대기
echo "백엔드 서버 준비 대기 중..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/kpi/factory/summary >/dev/null 2>&1; then
        echo -e "${GREEN}✅ 백엔드 서버 준비 완료${NC}"
        break
    fi
    echo -n "."
    sleep 2
    
    if [ $i -eq 30 ]; then
        handle_error "백엔드 서버가 60초 내에 시작되지 않았습니다."
    fi
done

# 5. 데이터 수집기 시작
echo -e "\n${YELLOW}5️⃣ 데이터 수집기 시작${NC}"
cd data_collector
python3 main.py >../logs/data_collector.log 2>&1 &
DATA_COLLECTOR_PID=$!
cd ..

echo -e "${GREEN}✅ 데이터 수집기 시작 완료 (PID: $DATA_COLLECTOR_PID)${NC}"
sleep 3

# 6. MQTT 시뮬레이터 시작
echo -e "\n${YELLOW}6️⃣ MQTT 시뮬레이터 시작 (15개 스테이션)${NC}"
cd mosquitto_MQTT
python3 run_simulation.py >../logs/mqtt_simulator.log 2>&1 &
MQTT_SIMULATOR_PID=$!
cd ..

echo -e "${GREEN}✅ MQTT 시뮬레이터 시작 완료 (PID: $MQTT_SIMULATOR_PID)${NC}"
sleep 5

# 7. React 프론트엔드 시작
echo -e "\n${YELLOW}7️⃣ React 프론트엔드 시작${NC}"
cd dashboard_frontend
npm run dev >../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

echo -e "${GREEN}✅ React 프론트엔드 시작 완료 (PID: $FRONTEND_PID)${NC}"

# 프론트엔드 준비 대기
echo "프론트엔드 서버 준비 대기 중..."
for i in {1..15}; do
    if curl -s http://localhost:5173 >/dev/null 2>&1; then
        echo -e "${GREEN}✅ 프론트엔드 서버 준비 완료${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

# 8. 시스템 상태 확인
echo -e "\n${YELLOW}8️⃣ 시스템 상태 확인${NC}"
echo "백엔드 API 테스트 중..."

if python3 dashboard_backend/test_api.py >logs/api_test.log 2>&1; then
    echo -e "${GREEN}✅ API 테스트 통과${NC}"
else
    echo -e "${YELLOW}⚠️  API 테스트 실패 - 로그 확인: logs/api_test.log${NC}"
fi

# 9. 브라우저 열기
echo -e "\n${BLUE}🌐 웹 브라우저에서 대시보드를 열까요? (y/n)${NC}"
read -r response

if [[ "$response" == "y" || "$response" == "Y" ]]; then
    open http://localhost:5173
fi

# 최종 상태 출력
echo -e "\n${GREEN}🎉 시스템 시작 완료!${NC}"
echo "============================================="
echo -e "${BLUE}📊 접속 주소:${NC}"
echo "  • 대시보드: http://localhost:5173"
echo "  • 백엔드 API: http://localhost:8080/api/kpi/factory/summary"
echo ""
echo -e "${BLUE}📋 실행 중인 서비스:${NC}"
echo "  • Mosquitto MQTT (PID: $MOSQUITTO_PID)"
echo "  • Spring Boot Backend (PID: $BACKEND_PID)"
echo "  • Data Collector (PID: $DATA_COLLECTOR_PID)"
echo "  • MQTT Simulator (PID: $MQTT_SIMULATOR_PID)"
echo "  • React Frontend (PID: $FRONTEND_PID)"
echo ""
echo -e "${BLUE}📁 로그 파일:${NC}"
echo "  • 백엔드: logs/backend.log"
echo "  • 데이터수집: logs/data_collector.log"
echo "  • 시뮬레이터: logs/mqtt_simulator.log"
echo "  • 프론트엔드: logs/frontend.log"
echo "  • API테스트: logs/api_test.log"
echo ""
echo -e "${YELLOW}🛑 종료하려면 Ctrl+C를 누르세요${NC}"
echo -e "${BLUE}💡 로그 실시간 보기: tail -f logs/[파일명]${NC}"
echo -e "${BLUE}💡 종료 후 가상환경 비활성화: deactivate${NC}"

# 무한 대기
while true; do
    sleep 1
done