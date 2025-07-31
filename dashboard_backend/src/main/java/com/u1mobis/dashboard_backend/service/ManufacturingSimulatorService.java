package com.u1mobis.dashboard_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.u1mobis.dashboard_backend.entity.*;
import com.u1mobis.dashboard_backend.repository.*;
import com.u1mobis.dashboard_backend.mqtt.MQTTPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ManufacturingSimulatorService {
    
    // 기존 서비스들 활용
    private final ProductionService productionService;
    private final EnvironmentService environmentService;
    private final ConveyorService conveyorService;
    private final KPICalculationService kpiCalculationService;
    
    // MQTT Publisher
    private final MQTTPublisher mqttPublisher;
    
    // 새로 추가된 Repository들
    private final ProductDetailRepository productDetailRepository;
    private final StationStatusRepository stationStatusRepository;
    private final RobotPositionRepository robotPositionRepository;
    private final ConveyorControlRepository conveyorControlRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ProductionLineRepository productionLineRepository;
    private final CompanyRepository companyRepository;
    private final CurrentProductionRepository currentProductionRepository;
    
    // 회사별 + 라인별 시뮬레이션 상태 관리
    // Key: "companyId_lineId"
    private final Map<String, SimulationState> activeSimulations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    
    // 시뮬레이션 상태 클래스
    public static class SimulationState {
        public boolean isRunning = false;
        public Long companyId;
        public Long lineId;
        public int productionCount = 0;
        public LocalDateTime startTime;
        public String companyCode;  // MQTT 토픽용 회사 코드
        public String[] availableColors = {"Black", "Gray", "Red"}; // Unity 프리팹과 정확히 일치
        public String[] availableDoorColors = {"Black", "Gray", "Red"}; // Unity 프리팹과 일치
        public Random random = new Random();
    }
    
    /**
     * 회사의 모든 라인에 대해 시뮬레이션 시작
     */
    public void startSimulation(Long companyId) {
        try {
            // 회사의 활성 라인들 조회
            List<ProductionLine> activeLines = productionLineRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId);
            
            if (activeLines.isEmpty()) {
                log.warn("회사 ID {}에 활성 라인이 없습니다", companyId);
                return;
            }
            
            // 각 라인별로 시뮬레이션 시작
            for (ProductionLine line : activeLines) {
                startLineSimulation(companyId, line.getLineId());
            }
            
            log.info("회사 ID {} - {}개 라인 시뮬레이션 시작", companyId, activeLines.size());
            
        } catch (Exception e) {
            log.error("시뮬레이션 시작 실패 - 회사 ID: {}", companyId, e);
        }
    }
    
    /**
     * 특정 라인의 시뮬레이션 시작
     */
    public void startLineSimulation(Long companyId, Long lineId) {
        String simulationKey = companyId + "_" + lineId;
        
        // 이미 실행 중인지 확인
        if (activeSimulations.containsKey(simulationKey) && 
            activeSimulations.get(simulationKey).isRunning) {
            log.info("라인 시뮬레이션이 이미 실행 중입니다 - 회사: {}, 라인: {}", companyId, lineId);
            return;
        }
        
        // 시뮬레이션 상태 초기화
        SimulationState state = new SimulationState();
        state.companyId = companyId;
        state.lineId = lineId;
        state.isRunning = true;
        state.startTime = LocalDateTime.now();
        state.companyCode = getCompanyCodeById(companyId);  // 회사 코드 설정
        activeSimulations.put(simulationKey, state);
        
        // 초기 데이터 설정
        initializeLineData(companyId, lineId);
        
        // 주기적 시뮬레이션 작업들 스케줄링
        scheduleSimulationTasks(simulationKey);
        
        log.info("라인 시뮬레이션 시작 - 회사: {}, 라인: {}", companyId, lineId);
    }
    
    /**
     * 라인 초기 데이터 설정
     */
    private void initializeLineData(Long companyId, Long lineId) {
        try {
            // 1. 스테이션 상태 초기화
            initializeStationStatus(lineId);
            
            // 2. 로봇 위치 초기화
            initializeRobotPositions(lineId);
            
            // 3. 컨베이어 제어 초기화
            initializeConveyorControl(lineId);
            
            // 4. 생산 계획 초기화
            initializeProductionPlan(lineId);
            
            // 5. 초기 제품 생성
            createInitialProducts(companyId, lineId);
            
        } catch (Exception e) {
            log.error("라인 초기 데이터 설정 실패 - 라인 ID: {}", lineId, e);
        }
    }
    
    /**
     * 스테이션 상태 초기화
     */
    private void initializeStationStatus(Long lineId) {
        // DoorStation 초기화
        StationStatus doorStation = StationStatus.builder()
            .stationId("DoorStation_L" + lineId)
            .stationName("문 조립 스테이션")
            .lineId(lineId)
            .status("OPERATING")
            .temperature(25.0 + Math.random() * 5)  // 25-30도
            .pressure(1.0 + Math.random() * 0.2)    // 1.0-1.2
            .efficiency(85.0 + Math.random() * 10)  // 85-95%
            .equipmentStatus("NORMAL")
            .cycleTime(120)
            .build();
        stationStatusRepository.save(doorStation);
        
        // WaterLeakTestStation 초기화
        StationStatus waterLeakStation = StationStatus.builder()
            .stationId("WaterLeakTestStation_L" + lineId)
            .stationName("누수 테스트 스테이션")
            .lineId(lineId)
            .status("OPERATING")
            .temperature(22.0 + Math.random() * 3)  // 22-25도
            .pressure(0.8 + Math.random() * 0.3)    // 0.8-1.1
            .efficiency(88.0 + Math.random() * 7)   // 88-95%
            .equipmentStatus("NORMAL")
            .cycleTime(180)
            .build();
        stationStatusRepository.save(waterLeakStation);
    }
    
    /**
     * 로봇 위치 초기화
     */
    private void initializeRobotPositions(Long lineId) {
        Random random = new Random();
        
        // 라인별 로봇 2개씩 배치 (문 조립용, 누수 테스트용)
        double lineOffset = lineId * 200;  // 라인별 Y축 간격
        
        // Door 조립 로봇
        RobotPosition doorRobot = RobotPosition.builder()
            .robotId("L" + lineId + "_ROBOT_DOOR")
            .lineId(lineId)
            .positionX(100.0)
            .positionY(lineOffset + 50.0)
            .positionZ(0.0)
            .batteryLevel(80 + random.nextInt(20))  // 80-100%
            .currentAction("WORKING")
            .movementSpeed(1.5)
            .isActive(true)
            .lastActionTime(LocalDateTime.now())
            .build();
        robotPositionRepository.save(doorRobot);
        
        // WaterLeak 테스트 로봇
        RobotPosition waterLeakRobot = RobotPosition.builder()
            .robotId("L" + lineId + "_ROBOT_WATER")
            .lineId(lineId)
            .positionX(300.0)
            .positionY(lineOffset + 50.0)
            .positionZ(0.0)
            .batteryLevel(80 + random.nextInt(20))  // 80-100%
            .currentAction("IDLE")
            .movementSpeed(1.2)
            .isActive(true)
            .lastActionTime(LocalDateTime.now())
            .build();
        robotPositionRepository.save(waterLeakRobot);
    }
    
    /**
     * 컨베이어 제어 초기화
     */
    private void initializeConveyorControl(Long lineId) {
        Random random = new Random();
        
        ConveyorControl conveyor = ConveyorControl.builder()
            .conveyorId("CONV_L" + lineId + "_MAIN")
            .conveyorName("라인 " + lineId + " 메인 컨베이어")
            .lineId(lineId)
            .command("START")
            .speed(1.0 + random.nextDouble() * 1.0)  // 1.0-2.0 m/s
            .direction("FORWARD")
            .sensorStatus(false)
            .reason("자동 생산")
            .isEmergency(false)
            .maintenanceMode(false)
            .totalRuntime(0L)
            .lastCommandTime(LocalDateTime.now())
            .build();
        conveyorControlRepository.save(conveyor);
    }
    
    /**
     * 생산 계획 초기화
     */
    private void initializeProductionPlan(Long lineId) {
        LocalDate today = LocalDate.now();
        
        // 오늘 계획이 이미 있는지 확인
        Optional<ProductionPlan> existingPlan = productionPlanRepository.findTodayPlanByLineId(lineId, today);
        
        if (existingPlan.isEmpty()) {
            ProductionPlan todayPlan = ProductionPlan.builder()
                .lineId(lineId)
                .planDate(today)
                .dailyTarget(50 + new Random().nextInt(30))  // 50-80개
                .currentProgress(0)
                .productionRate(12)  // 시간당 12개
                .shift("주간")
                .status("IN_PROGRESS")
                .efficiency(85.0)
                .build();
            todayPlan.startProduction();
            productionPlanRepository.save(todayPlan);
        }
    }
    
    /**
     * 초기 제품 생성
     */
    private void createInitialProducts(Long companyId, Long lineId) {
        SimulationState state = activeSimulations.get(companyId + "_" + lineId);
        if (state == null) return;
        
        // 라인당 2-3개 초기 제품 생성
        int initialProductCount = 2 + state.random.nextInt(2);
        
        for (int i = 0; i < initialProductCount; i++) {
            createNewProduct(companyId, lineId, state);
        }
    }
    
    /**
     * 새 제품 생성
     */
    private void createNewProduct(Long companyId, Long lineId, SimulationState state) {
        try {
            state.productionCount++;
            String productId = "CAR_Line" + lineId + "_" + String.format("%03d", state.productionCount); // Unity 패턴과 일치
            
            // CurrentProduction 생성 (기존 서비스 활용)
            String companyName = getCompanyName(companyId);
            if (companyName != null) {
                // 1. CurrentProduction 생성
                CurrentProduction production = CurrentProduction.builder()
                    .productId(productId)
                    .productColor(state.availableColors[state.random.nextInt(state.availableColors.length)])
                    .startTime(LocalDateTime.now())
                    .dueDate(LocalDateTime.now().plusHours(8))
                    .reworkCount(0)
                    .currentStation("DoorStation")
                    .status("PROCESSING")
                    .lineId(lineId)
                    .targetQuantity(1)
                    .build();
                currentProductionRepository.save(production);
                
                // 2. ProductDetail 생성 - Unity 좌표계에 맞게 조정
                double lineOffset = lineId * 20;  // Unity 스케일에 맞게 축소
                String doorColor = state.availableDoorColors[state.random.nextInt(state.availableDoorColors.length)];
                int workProgress = 5 + state.random.nextInt(10);  // 5-15% 시작
                double positionX = -10.0 + state.random.nextDouble() * 20;  // Unity 좌표계
                double positionY = 0.5;  // 바닥에서 살짝 위
                double positionZ = lineOffset + state.random.nextDouble() * 10;  // 라인별 Z축 위치
                
                ProductDetail detail = ProductDetail.builder()
                    .productId(productId)
                    .doorColor(doorColor)
                    .workProgress(workProgress)
                    .estimatedCompletion(LocalDateTime.now().plusHours(6))
                    .positionX(positionX)
                    .positionY(positionY)
                    .positionZ(positionZ)  // Z축 위치 설정
                    .lineId(lineId)
                    .build();
                productDetailRepository.save(detail);
                
                // 3. MQTT로 생산 시작 및 제품 상세 정보 발송 (올바른 토픽 패턴 사용)
                if (state.companyCode != null) {
                    mqttPublisher.publishProductionStarted(state.companyCode, lineId, productId, 
                        production.getTargetQuantity(), production.getDueDate().toString());
                    
                    mqttPublisher.publishProductDetails(state.companyCode, lineId, productId, 
                        production.getProductColor(), doorColor, workProgress, positionX, positionZ);
                        
                    // KPI 운영 데이터도 주기적으로 발송
                    Random random = new Random();
                    mqttPublisher.publishOperationsData(state.companyCode, lineId, 
                        480, // 8시간 = 480분
                        random.nextInt(30), // 0-30분 다운타임
                        120.0, // 목표 사이클 타임 120초
                        1, // 양품 1개
                        1, // 총 1개
                        1, // 일회 통과 1개
                        1  // 정시 납기 1개
                    );
                }
                
                log.debug("새 제품 생성 완료 - 제품 ID: {}, 라인: {}", productId, lineId);
            }
            
        } catch (Exception e) {
            log.error("제품 생성 실패 - 라인 ID: {}", lineId, e);
        }
    }
    
    /**
     * 주기적 시뮬레이션 작업 스케줄링
     */
    private void scheduleSimulationTasks(String simulationKey) {
        SimulationState state = activeSimulations.get(simulationKey);
        if (state == null) return;
        
        // 1. 환경 데이터 업데이트 (5초마다)
        scheduler.scheduleAtFixedRate(() -> updateEnvironmentData(state), 5, 5, TimeUnit.SECONDS);
        
        // 2. 제품 이동 처리 (10초마다)
        scheduler.scheduleAtFixedRate(() -> moveProducts(state), 10, 10, TimeUnit.SECONDS);
        
        // 3. 로봇 상태 업데이트 (7초마다)
        scheduler.scheduleAtFixedRate(() -> updateRobotStatus(state), 7, 7, TimeUnit.SECONDS);
        
        // 4. 컨베이어 상태 업데이트 (15초마다)
        scheduler.scheduleAtFixedRate(() -> updateConveyorStatus(state), 15, 15, TimeUnit.SECONDS);
        
        // 5. 새 제품 생성 (30초마다)
        scheduler.scheduleAtFixedRate(() -> generateNewProduct(state), 30, 30, TimeUnit.SECONDS);
        
        // 6. 생산 완료 처리 (20초마다)
        scheduler.scheduleAtFixedRate(() -> completeProducts(state), 20, 20, TimeUnit.SECONDS);
    }
    
    /**
     * 환경 데이터 업데이트 (기존 EnvironmentService 활용 + MQTT 발송)
     */
    private void updateEnvironmentData(SimulationState state) {
        try {
            String companyName = getCompanyName(state.companyId);
            if (companyName != null && state.companyCode != null) {
                double temperature = 20.0 + state.random.nextDouble() * 10;  // 20-30도
                double humidity = 40.0 + state.random.nextDouble() * 20;     // 40-60%
                int airQuality = 50 + state.random.nextInt(100);             // 50-150
                
                // 기존 서비스로 저장
                environmentService.saveEnvironmentData(companyName, temperature, humidity, airQuality);
                
                // MQTT로 실시간 발송
                mqttPublisher.publishEnvironmentData(state.companyCode, temperature, humidity, airQuality);
            }
        } catch (Exception e) {
            log.error("환경 데이터 업데이트 실패 - 라인: {}", state.lineId, e);
        }
    }
    
    /**
     * 제품 이동 처리
     */
    private void moveProducts(SimulationState state) {
        try {
            // 현재 DoorStation에 있는 제품들 조회
            List<CurrentProduction> doorProducts = currentProductionRepository
                .findByLineIdAndCurrentStation(state.lineId, "DoorStation");
            
            // 일부 제품을 WaterLeakTestStation으로 이동
            for (CurrentProduction product : doorProducts) {
                if (state.random.nextDouble() < 0.3) {  // 30% 확률로 이동
                    // CurrentProduction 업데이트
                    product.setCurrentStation("WaterLeakTestStation");
                    currentProductionRepository.save(product);
                    
                    // ProductDetail 위치 업데이트
                    Optional<ProductDetail> detailOpt = productDetailRepository.findById(product.getProductId());
                    if (detailOpt.isPresent()) {
                        ProductDetail detail = detailOpt.get();
                        double lineOffset = state.lineId * 20;  // Unity 스케일
                        // WaterLeakTestStation으로 이동 (Unity 좌표계)
                        detail.setUnityPosition(20.0 + state.random.nextDouble() * 10, 
                                              0.5, 
                                              lineOffset + state.random.nextDouble() * 10);
                        detail.updateProgress(detail.getWorkProgress() + 20 + state.random.nextInt(30));
                        productDetailRepository.save(detail);
                    }
                    
                    // 스테이션 상태 업데이트
                    Optional<StationStatus> stationOpt = stationStatusRepository
                        .findById("WaterLeakTestStation_L" + state.lineId);
                    if (stationOpt.isPresent()) {
                        StationStatus station = stationOpt.get();
                        station.changeStatus("OPERATING", "CAR_" + product.getProductId().split("_")[2]);
                        stationStatusRepository.save(station);
                    }
                    
                    log.debug("제품 이동 완료 - 제품: {}, DoorStation → WaterLeakTestStation", product.getProductId());
                }
            }
            
        } catch (Exception e) {
            log.error("제품 이동 처리 실패 - 라인: {}", state.lineId, e);
        }
    }
    
    /**
     * 로봇 상태 업데이트
     */
    private void updateRobotStatus(SimulationState state) {
        try {
            List<RobotPosition> robots = robotPositionRepository.findByLineIdAndIsActiveTrue(state.lineId);
            
            for (RobotPosition robot : robots) {
                // 배터리 소모 (1-3%)
                robot.consumeBattery(1 + state.random.nextInt(3));
                
                // 동작 상태 랜덤 변경
                String[] actions = {"WORKING", "MOVING", "IDLE"};
                if (state.random.nextDouble() < 0.2) {  // 20% 확률로 상태 변경
                    robot.changeAction(actions[state.random.nextInt(actions.length)]);
                }
                
                // 위치 약간 변경 (작업 중일 때)
                if ("WORKING".equals(robot.getCurrentAction())) {
                    double newX = robot.getPositionX() + (state.random.nextDouble() - 0.5) * 10;
                    double newY = robot.getPositionY() + (state.random.nextDouble() - 0.5) * 10;
                    robot.setPosition(newX, newY, robot.getPositionZ());
                }
                
                robotPositionRepository.save(robot);
            }
            
        } catch (Exception e) {
            log.error("로봇 상태 업데이트 실패 - 라인: {}", state.lineId, e);
        }
    }
    
    /**
     * 컨베이어 상태 업데이트
     */
    private void updateConveyorStatus(SimulationState state) {
        try {
            List<ConveyorControl> conveyors = conveyorControlRepository.findByLineId(state.lineId);
            
            for (ConveyorControl conveyor : conveyors) {
                // 속도 미세 조정
                if ("START".equals(conveyor.getCommand())) {
                    double newSpeed = 1.0 + state.random.nextDouble() * 1.0;  // 1.0-2.0
                    conveyor.setSpeed(newSpeed);
                }
                
                // 센서 상태 랜덤 변경
                conveyor.updateSensorStatus(state.random.nextDouble() < 0.4);  // 40% 확률로 감지
                
                conveyorControlRepository.save(conveyor);
                
                // MQTT로 컨베이어 상태 발송
                if (state.companyCode != null) {
                    mqttPublisher.publishConveyorData(state.companyCode, state.lineId,
                        conveyor.getCommand(), "자동 운영");
                }
            }
            
        } catch (Exception e) {
            log.error("컨베이어 상태 업데이트 실패 - 라인: {}", state.lineId, e);
        }
    }
    
    /**
     * 새 제품 생성
     */
    private void generateNewProduct(SimulationState state) {
        try {
            // 현재 진행 중인 제품 수 확인
            long currentProductCount = currentProductionRepository.countByLineIdAndStatus(state.lineId, "PROCESSING");
            
            // 라인당 최대 5개까지만 유지
            if (currentProductCount < 5 && state.random.nextDouble() < 0.7) {  // 70% 확률로 생성
                createNewProduct(state.companyId, state.lineId, state);
            }
            
        } catch (Exception e) {
            log.error("새 제품 생성 실패 - 라인: {}", state.lineId, e);
        }
    }
    
    /**
     * 생산 완료 처리
     */
    private void completeProducts(SimulationState state) {
        try {
            // WaterLeakTestStation에서 완료될 제품들 조회
            List<CurrentProduction> waterLeakProducts = currentProductionRepository
                .findByLineIdAndCurrentStation(state.lineId, "WaterLeakTestStation");
            
            for (CurrentProduction product : waterLeakProducts) {
                // ProductDetail 진행률 확인
                Optional<ProductDetail> detailOpt = productDetailRepository.findById(product.getProductId());
                
                if (detailOpt.isPresent() && detailOpt.get().getWorkProgress() >= 80) {
                    if (state.random.nextDouble() < 0.25) {  // 25% 확률로 완료
                        // 기존 ProductionService 활용하여 완료 처리
                        String companyName = getCompanyName(state.companyId);
                        if (companyName != null) {
                            double cycleTime = 300 + state.random.nextDouble() * 120;  // 300-420초
                            String quality = state.random.nextDouble() < 0.95 ? "PASS" : "FAIL";
                            
                            productionService.completeProduction(companyName, state.lineId, 
                                product.getProductId(), cycleTime, quality, product.getDueDate());
                            
                            // MQTT로 생산 완료 알림 발송
                            if (state.companyCode != null) {
                                mqttPublisher.publishProductionCompleted(state.companyCode, state.lineId, 
                                    product.getProductId(), cycleTime, quality, product.getDueDate().toString());
                            }
                            
                            // ProductDetail 진행률 100% 완료
                            ProductDetail detail = detailOpt.get();
                            detail.updateProgress(100);
                            productDetailRepository.save(detail);
                            
                            // 생산 계획 진행률 업데이트
                            updateProductionPlanProgress(state.lineId);
                            
                            log.debug("제품 생산 완료 - 제품 ID: {}, 품질: {}", product.getProductId(), quality);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("생산 완료 처리 실패 - 라인: {}", state.lineId, e);
        }
    }
    
    /**
     * 생산 계획 진행률 업데이트
     */
    private void updateProductionPlanProgress(Long lineId) {
        try {
            Optional<ProductionPlan> planOpt = productionPlanRepository.findTodayPlanByLineId(lineId, LocalDate.now());
            
            if (planOpt.isPresent()) {
                ProductionPlan plan = planOpt.get();
                plan.updateProgress(plan.getCurrentProgress() + 1);
                productionPlanRepository.save(plan);
            }
            
        } catch (Exception e) {
            log.error("생산 계획 업데이트 실패 - 라인: {}", lineId, e);
        }
    }
    
    /**
     * 시뮬레이션 중지
     */
    public void stopSimulation(Long companyId) {
        List<ProductionLine> lines = productionLineRepository.findByCompanyCompanyId(companyId);
        
        for (ProductionLine line : lines) {
            stopLineSimulation(companyId, line.getLineId());
        }
        
        log.info("회사 ID {} 시뮬레이션 중지", companyId);
    }
    
    /**
     * 라인별 시뮬레이션 중지
     */
    public void stopLineSimulation(Long companyId, Long lineId) {
        String simulationKey = companyId + "_" + lineId;
        SimulationState state = activeSimulations.get(simulationKey);
        
        if (state != null) {
            state.isRunning = false;
            activeSimulations.remove(simulationKey);
            log.info("라인 시뮬레이션 중지 - 회사: {}, 라인: {}", companyId, lineId);
        }
    }
    
    /**
     * 시뮬레이션 상태 조회
     */
    public Map<String, Object> getSimulationStatus(Long companyId) {
        List<ProductionLine> lines = productionLineRepository.findByCompanyCompanyId(companyId);
        Map<String, Object> status = new HashMap<>();
        
        for (ProductionLine line : lines) {
            String simulationKey = companyId + "_" + line.getLineId();
            SimulationState state = activeSimulations.get(simulationKey);
            
            Map<String, Object> lineStatus = new HashMap<>();
            lineStatus.put("lineId", line.getLineId());
            lineStatus.put("lineName", line.getLineName());
            lineStatus.put("isRunning", state != null && state.isRunning);
            
            if (state != null) {
                lineStatus.put("startTime", state.startTime);
                lineStatus.put("productionCount", state.productionCount);
            }
            
            status.put("line_" + line.getLineId(), lineStatus);
        }
        
        return status;
    }
    
    /**
     * 회사명 조회 헬퍼 메서드
     */
    private String getCompanyName(Long companyId) {
        try {
            return companyRepository.findById(companyId)
                .map(Company::getCompanyName)
                .orElse(null);
        } catch (Exception e) {
            log.error("회사명 조회 실패 - 회사 ID: {}", companyId, e);
            return null;
        }
    }
    
    /**
     * 회사 코드 조회 헬퍼 메서드 (MQTT 토픽용)
     */
    private String getCompanyCodeById(Long companyId) {
        try {
            return companyRepository.findById(companyId)
                .map(Company::getCompanyCode)
                .orElse("ms");  // 기본값
        } catch (Exception e) {
            log.error("회사 코드 조회 실패 - 회사 ID: {}", companyId, e);
            return "ms";  // 기본값
        }
    }
}