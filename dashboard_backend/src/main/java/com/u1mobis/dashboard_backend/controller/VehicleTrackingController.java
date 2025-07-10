package com.u1mobis.dashboard_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.u1mobis.dashboard_backend.service.VehicleTrackingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/digital-twin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
@Slf4j
public class VehicleTrackingController {
    
    private final VehicleTrackingService vehicleTrackingService;
    
    /**
     * Data Collector에서 차량 추적 데이터 수신
     */
    @PostMapping("/vehicles")
    public ResponseEntity<Map<String, String>> receiveVehicleData(@RequestBody Map<String, Object> vehicleData) {
        try {
            log.info("차량 추적 데이터 수신: {} 대의 차량", vehicleData.get("total_vehicles"));
            
            vehicleTrackingService.processVehicleTrackingData(vehicleData);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "차량 추적 데이터가 성공적으로 처리되었습니다.",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("차량 추적 데이터 처리 중 오류 발생", e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "차량 추적 데이터 처리 중 오류가 발생했습니다: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 실시간 차량 위치 데이터 조회
     */
    @GetMapping("/vehicles")
    public ResponseEntity<Map<String, Object>> getCurrentVehicles() {
        try {
            Map<String, Object> vehicleData = vehicleTrackingService.getCurrentVehicleData();
            
            return ResponseEntity.ok(vehicleData);
            
        } catch (Exception e) {
            log.error("차량 데이터 조회 중 오류 발생", e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "차량 데이터 조회 중 오류가 발생했습니다: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 특정 차량 상세 정보 조회
     */
    @GetMapping("/vehicles/{vehicleId}")
    public ResponseEntity<Map<String, Object>> getVehicleDetails(@PathVariable String vehicleId) {
        try {
            Map<String, Object> vehicleDetails = vehicleTrackingService.getVehicleDetails(vehicleId);
            
            if (vehicleDetails == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(vehicleDetails);
            
        } catch (Exception e) {
            log.error("차량 상세 정보 조회 중 오류 발생", e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "차량 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 특정 스테이션의 차량 목록 조회
     */
    @GetMapping("/stations/{stationId}/vehicles")
    public ResponseEntity<List<Map<String, Object>>> getVehiclesByStation(@PathVariable String stationId) {
        try {
            List<Map<String, Object>> vehicles = vehicleTrackingService.getVehiclesByStation(stationId);
            
            return ResponseEntity.ok(vehicles);
            
        } catch (Exception e) {
            log.error("스테이션별 차량 조회 중 오류 발생", e);
            
            return ResponseEntity.status(500).body(List.of(Map.of(
                "status", "error",
                "message", "스테이션별 차량 조회 중 오류가 발생했습니다: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            )));
        }
    }
    
    /**
     * 생산 통계 데이터 조회
     */
    @GetMapping("/production-stats")
    public ResponseEntity<Map<String, Object>> getProductionStats() {
        try {
            Map<String, Object> stats = vehicleTrackingService.getProductionStatistics();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("생산 통계 조회 중 오류 발생", e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "생산 통계 조회 중 오류가 발생했습니다: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
}