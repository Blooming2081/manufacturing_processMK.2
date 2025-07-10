package com.u1mobis.dashboard_backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.u1mobis.dashboard_backend.entity.StationStatus;
import com.u1mobis.dashboard_backend.service.StationStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/station")
@RequiredArgsConstructor
@Slf4j
public class StationStatusController {
    
    @Autowired
    private final StationStatusService stationStatusService;
    
    /**
     * MQTT 상태 데이터 수신
     */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> receiveStatusData(@RequestBody Map<String, Object> statusData) {
        try {
            log.info("스테이션 상태 데이터 수신: {}", statusData);
            
            StationStatus savedStatus = stationStatusService.processStatusData(statusData);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "상태 데이터 저장 완료",
                "statusId", savedStatus.getId(),
                "stationId", savedStatus.getStationId(),
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("상태 데이터 처리 실패: ", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "데이터 처리 실패: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
    
    /**
     * 특정 스테이션의 최신 상태 조회
     */
    @GetMapping("/{stationId}/status")
    public ResponseEntity<StationStatus> getLatestStatus(@PathVariable String stationId) {
        try {
            StationStatus status = stationStatusService.getLatestStatus(stationId);
            if (status != null) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("최신 상태 조회 실패: ", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 모든 스테이션의 최신 상태 조회
     */
    @GetMapping("/status/all")
    public ResponseEntity<List<StationStatus>> getAllLatestStatus() {
        try {
            List<StationStatus> allStatus = stationStatusService.getAllLatestStatus();
            return ResponseEntity.ok(allStatus);
        } catch (Exception e) {
            log.error("전체 상태 조회 실패: ", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 상태별 스테이션 조회
     */
    @GetMapping("/status")
    public ResponseEntity<List<StationStatus>> getStationsByStatus(@RequestParam String status) {
        try {
            List<StationStatus> stations = stationStatusService.getStationsByStatus(status);
            return ResponseEntity.ok(stations);
        } catch (Exception e) {
            log.error("상태별 스테이션 조회 실패: ", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 실행 중인 스테이션 조회
     */
    @GetMapping("/running")
    public ResponseEntity<List<StationStatus>> getRunningStations() {
        try {
            List<StationStatus> runningStations = stationStatusService.getRunningStations();
            return ResponseEntity.ok(runningStations);
        } catch (Exception e) {
            log.error("실행 중인 스테이션 조회 실패: ", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 스테이션 통계 조회
     */
    @GetMapping("/{stationId}/stats")
    public ResponseEntity<Map<String, Object>> getStationStats(@PathVariable String stationId) {
        try {
            Map<String, Object> stats = stationStatusService.getStationStatistics(stationId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("스테이션 통계 조회 실패: ", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 시스템 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            Map<String, Object> health = stationStatusService.getSystemHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("시스템 상태 확인 실패: ", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
    
    /**
     * 데이터베이스 연결 테스트
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        try {
            boolean isConnected = stationStatusService.testDatabaseConnection();
            long totalCount = stationStatusService.getTotalStatusCount();
            
            return ResponseEntity.ok(Map.of(
                "database_connected", isConnected,
                "total_records", totalCount,
                "timestamp", LocalDateTime.now(),
                "message", isConnected ? "데이터베이스 연결 성공" : "데이터베이스 연결 실패"
            ));
        } catch (Exception e) {
            log.error("데이터베이스 테스트 실패: ", e);
            return ResponseEntity.status(500).body(Map.of(
                "database_connected", false,
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
}