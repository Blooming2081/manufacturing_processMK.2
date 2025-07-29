package com.u1mobis.dashboard_backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.u1mobis.dashboard_backend.entity.KPIData;
import com.u1mobis.dashboard_backend.service.EnvironmentService;
import com.u1mobis.dashboard_backend.service.KPICalculationService;
import com.u1mobis.dashboard_backend.service.ProductionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/{companyName}")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final ProductionService productionService;
    private final KPICalculationService kpiCalculationService;
    private final EnvironmentService environmentService;

    // 대시보드 메인 데이터
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(@PathVariable String companyName) {
        log.info("대시보드 데이터 요청 - 회사: {}", companyName);
        Map<String, Object> dashboard = new HashMap<>();

        // 생산 현황
        dashboard.put("production", productionService.getCurrentProductionStatus());

        // KPI 데이터
        dashboard.put("kpi", kpiCalculationService.getRealTimeKPI());

        return ResponseEntity.ok(dashboard);
    }

    // 실시간 KPI 조회
    @GetMapping("/kpi/realtime")
    public ResponseEntity<Map<String, Object>> getRealTimeKPI(@PathVariable String companyName) {
        log.info("실시간 KPI 요청 - 회사: {}", companyName);
        return ResponseEntity.ok(kpiCalculationService.getRealTimeKPI());
    }

    // 생산 현황 조회
    @GetMapping("/production/status")
    public ResponseEntity<Map<String, Object>> getProductionStatus(@PathVariable String companyName) {
        log.info("생산 현황 요청 - 회사: {}", companyName);
        return ResponseEntity.ok(productionService.getCurrentProductionStatus());
    }

    /**
     * KPI 데이터 처리 (테스트용) - 타입 안전 처리
     */
    @PostMapping("/kpi/process")
    public ResponseEntity<Map<String, Object>> processKPIData(@PathVariable String companyName, @RequestBody Map<String, Object> kpiData) {
        log.info("KPI 데이터 처리 요청 - 회사: {}", companyName);
        try {
            log.info("KPI 데이터 수신 - 회사: {}, 데이터: {}", companyName, kpiData);

            // 안전한 타입 변환
            Integer plannedTime = convertToInteger(kpiData.get("planned_time"));
            Integer downtime = convertToInteger(kpiData.get("downtime"));
            Double targetCycleTime = convertToDouble(kpiData.get("target_cycle_time"));
            Integer goodCount = convertToInteger(kpiData.get("good_count"));
            Integer totalCount = convertToInteger(kpiData.get("total_count"));
            Integer firstTimePassCount = convertToInteger(kpiData.get("first_time_pass_count"));
            Integer onTimeDeliveryCount = convertToInteger(kpiData.get("on_time_delivery_count"));

            KPIData result = kpiCalculationService.processKPIData(
                    companyName, 1L, plannedTime, downtime, targetCycleTime,
                    goodCount, totalCount, firstTimePassCount, onTimeDeliveryCount);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "calculated_oee", result.getCalculatedOEE(),
                    "calculated_fty", result.getCalculatedFTY(),
                    "calculated_otd", result.getCalculatedOTD(),
                    "timestamp", result.getTimestamp().toString()));

        } catch (Exception e) {
            log.error("KPI 데이터 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    // 헬퍼 메서드들 추가
    private Integer convertToInteger(Object value) {
        if (value == null)
            return 0;
        if (value instanceof Integer)
            return (Integer) value;
        if (value instanceof Number)
            return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private Double convertToDouble(Object value) {
        if (value == null)
            return 0.0;
        if (value instanceof Double)
            return (Double) value;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }



}
