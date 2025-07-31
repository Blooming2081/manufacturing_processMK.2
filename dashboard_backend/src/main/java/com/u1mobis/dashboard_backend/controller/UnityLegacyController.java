
package com.u1mobis.dashboard_backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.u1mobis.dashboard_backend.service.ProductionService;
import com.u1mobis.dashboard_backend.service.ConveyorService;
import com.u1mobis.dashboard_backend.entity.CurrentProduction;
import com.u1mobis.dashboard_backend.entity.ProductionLine;
import com.u1mobis.dashboard_backend.entity.Company;
import com.u1mobis.dashboard_backend.repository.ProductDetailRepository;
import com.u1mobis.dashboard_backend.repository.ProductionLineRepository;
import com.u1mobis.dashboard_backend.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unity WebGL에서 직접 호출하는 Legacy API들
 * 인증 없이 호출되는 Unity 전용 엔드포인트
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
@Slf4j
public class UnityLegacyController {
    
    private final ProductionService productionService;
    private final ConveyorService conveyorService;
    private final ProductDetailRepository productDetailRepository;
    private final ProductionLineRepository productionLineRepository;
    private final CompanyRepository companyRepository;
    
    /**
     * Unity WebGL에서 호출하는 생산 주문 정보 (Legacy)
     */
    @GetMapping("/production/orders/line/{lineId}")
    public ResponseEntity<Map<String, Object>> getProductionOrdersByLine(@PathVariable String lineId) {
        try {
            log.info("Unity Legacy API 호출 - 생산 주문, 라인: {}", lineId);
            
            Map<String, Object> orders = new HashMap<>();
            orders.put("lineId", lineId);
            orders.put("orders", List.of(
                Map.of("orderId", "ORD_001", "productType", "DOOR", "quantity", 5, "status", "IN_PROGRESS"),
                Map.of("orderId", "ORD_002", "productType", "CHASSIS", "quantity", 3, "status", "PENDING")
            ));
            orders.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Unity 생산 주문 조회 실패 - 라인: {}", lineId, e);
            return ResponseEntity.status(500).body(Map.of("error", "생산 주문 조회 실패"));
        }
    }
    
    /**
     * Unity WebGL에서 호출하는 문 신호 정보 (Legacy)
     */
    @GetMapping("/production/door/signals/line/{lineId}")
    public ResponseEntity<Map<String, Object>> getDoorSignalsByLine(@PathVariable String lineId) {
        try {
            log.info("Unity Legacy API 호출 - 문 신호, 라인: {}", lineId);
            
            Map<String, Object> signals = new HashMap<>();
            signals.put("lineId", lineId);
            signals.put("signals", Map.of(
                "doorOpen", true,
                "doorClosed", false,
                "sensorDetected", true,
                "emergencyStop", false,
                "cycleComplete", false
            ));
            signals.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(signals);
        } catch (Exception e) {
            log.error("Unity 문 신호 조회 실패 - 라인: {}", lineId, e);
            return ResponseEntity.status(500).body(Map.of("error", "문 신호 조회 실패"));
        }
    }
    
    /**
     * Unity WebGL에서 호출하는 컨베이어 상태 (Legacy)
     */
    @GetMapping("/conveyor/status")
    public ResponseEntity<Map<String, Object>> getConveyorStatus() {
        try {
            log.info("Unity Legacy API 호출 - 컨베이어 상태");
            
            Map<String, Object> conveyorStatus = conveyorService.getCurrentConveyorStatus();
            return ResponseEntity.ok(conveyorStatus);
            
        } catch (Exception e) {
            log.error("Unity 컨베이어 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "컨베이어 상태 조회 실패"
            ));
        }
    }
    
    /**
     * Unity WebGL에서 호출하는 일반적인 생산 현황
     */
    @GetMapping("/production/status")
    public ResponseEntity<Map<String, Object>> getProductionStatus() {
        try {
            log.info("Unity Legacy API 호출 - 생산 현황");
            
            Map<String, Object> productionStatus = productionService.getCurrentProductionStatus();
            return ResponseEntity.ok(productionStatus);
            
        } catch (Exception e) {
            log.error("Unity 생산 현황 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "status", "error", 
                "message", "생산 현황 조회 실패"
            ));
        }
    }
    
    /**
     * Unity WebGL에서 호출하는 실시간 데이터 (Legacy) - 인증 없음
     */
    @GetMapping("/unity/realtime-data")
    public ResponseEntity<Map<String, Object>> getRealtimeData(
            @RequestParam(value = "companyName", defaultValue = "u1mobis") String companyName) {
        try {
            log.info("Unity Legacy API 호출 - 실시간 데이터, 회사: {}", companyName);
            
            Map<String, Object> unityData = new HashMap<>();

            // 1. 회사 정보 조회
            Company company = companyRepository.findByCompanyName(companyName)
                .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다: " + companyName));
            
            // 2. 해당 회사의 생산 라인 조회
            List<ProductionLine> companyLines = productionLineRepository.findByCompanyAndIsActiveTrue(company);
            List<Long> lineIds = companyLines.stream().map(ProductionLine::getLineId).toList();
            
            log.info("회사: {}, 활성 라인 수: {}, 라인 IDs: {}", companyName, lineIds.size(), lineIds);
            
            // 3. 전체 생산 현황 데이터 조회
            Map<String, Object> productionStatus = productionService.getCurrentProductionStatus();
            @SuppressWarnings("unchecked")
            List<CurrentProduction> allProducts = (List<CurrentProduction>) productionStatus.get("processing_products");
            
            // 4. 회사별 필터링 - 해당 회사의 라인에서만 생산되는 제품들
            List<CurrentProduction> processingProducts = new ArrayList<>();
            if (allProducts != null) {
                processingProducts = allProducts.stream()
                    .filter(product -> lineIds.contains(product.getLineId()))
                    .toList();
            }
            
            log.info("전체 제품 수: {}, 회사별 필터링 후: {}, 회사: {}", 
                allProducts != null ? allProducts.size() : 0, processingProducts.size(), companyName);
            
            // Unity용 제품 위치 데이터 변환
            Map<String, Object> products = new HashMap<>();
            if (processingProducts != null) {
                for (CurrentProduction product : processingProducts) {
                    String unityCarId = "CAR_Line" + product.getLineId() + "_" + String.format("%03d", product.getProductId());
                    
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("productId", product.getProductId());
                    productData.put("currentStation", product.getCurrentStation());
                    productData.put("status", product.getStatus());
                    productData.put("productColor", product.getProductColor() != null ? product.getProductColor() : "WHITE");
                    productData.put("lineId", product.getLineId());
                    
                    // 기본 위치 설정 (실제로는 위치 계산 로직 필요)
                    Map<String, Object> position = new HashMap<>();
                    position.put("x", product.getLineId() == 1 ? 10.0 : -10.0);
                    position.put("y", 0.0);
                    position.put("z", 5.0);
                    productData.put("position", position);
                    
                    products.put(unityCarId, productData);
                }
            }
            
            // 스테이션 데이터 (기본 데이터)
            Map<String, Object> stations = new HashMap<>();
            stations.put("DoorStation_Line1", Map.of(
                "status", "operating",
                "currentProduct", products.isEmpty() ? null : products.keySet().iterator().next(),
                "efficiency", 85.5
            ));
            stations.put("DoorStation_Line2", Map.of(
                "status", "idle", 
                "currentProduct", null,
                "efficiency", 92.0
            ));
            stations.put("WaterLeakTestStation_Line1", Map.of(
                "status", "idle",
                "currentProduct", null,
                "efficiency", 90.0
            ));
            stations.put("WaterLeakTestStation_Line2", Map.of(
                "status", "operating",
                "currentProduct", null,
                "efficiency", 88.5
            ));
            
            // 로봇 데이터 (기본 데이터)
            Map<String, Object> robots = new HashMap<>();
            robots.put("Robot_FrontRight_Line2", Map.of(
                "status", "active",
                "currentTask", "working", 
                "batteryLevel", 75
            ));
            robots.put("Robot_RearRight_Line2", Map.of(
                "status", "active",
                "currentTask", "idle",
                "batteryLevel", 82
            ));
            
            unityData.put("products", products);
            unityData.put("stations", stations);
            unityData.put("robots", robots);
            unityData.put("timestamp", System.currentTimeMillis());
            unityData.put("companyName", companyName);
            
            log.info("Unity 실시간 데이터 전송 완료 - 제품: {}, 스테이션: {}, 로봇: {}", 
                products.size(), stations.size(), robots.size());
            
            return ResponseEntity.ok(unityData);
            
        } catch (RuntimeException e) {
            log.error("Unity 실시간 데이터 조회 실패 - 회사: {}, 에러: {}", companyName, e.getMessage());
            
            // 회사를 찾을 수 없는 경우 빈 데이터 반환
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("products", new HashMap<>());
            emptyData.put("stations", new HashMap<>());
            emptyData.put("robots", new HashMap<>());
            emptyData.put("timestamp", System.currentTimeMillis());
            emptyData.put("companyName", companyName);
            emptyData.put("error", "회사별 데이터 조회 실패: " + e.getMessage());
            
            return ResponseEntity.ok(emptyData);
        } catch (Exception e) {
            log.error("Unity 실시간 데이터 조회 실패 - 회사: {}, 에러: {}", companyName, e.getMessage(), e);
            
            // 기타 에러 시 빈 데이터 반환
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("products", new HashMap<>());
            emptyData.put("stations", new HashMap<>());
            emptyData.put("robots", new HashMap<>());
            emptyData.put("timestamp", System.currentTimeMillis());
            emptyData.put("companyName", companyName);
            emptyData.put("error", "데이터 조회 실패: " + e.getMessage());
            
            return ResponseEntity.ok(emptyData);
        }
    }
    
    /**
     * Unity WebGL Health Check
     */
    @GetMapping("/unity/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "message", "Unity Legacy API is running",
            "timestamp", System.currentTimeMillis()
        ));
    }
}