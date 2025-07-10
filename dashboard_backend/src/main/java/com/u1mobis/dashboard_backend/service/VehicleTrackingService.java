package com.u1mobis.dashboard_backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VehicleTrackingService {
    
    // 메모리 기반 차량 데이터 저장소 (실제 환경에서는 Redis 또는 데이터베이스 사용)
    private final Map<String, Map<String, Object>> vehicleData = new ConcurrentHashMap<>();
    private Map<String, Object> latestTrackingData = new HashMap<>();
    private Map<String, Object> latestProductionStats = new HashMap<>();
    
    /**
     * MQTT에서 수신한 차량 추적 데이터 처리
     */
    @SuppressWarnings("unchecked")
    public void processVehicleTrackingData(Map<String, Object> trackingData) {
        try {
            // 전체 추적 데이터 저장
            this.latestTrackingData = new HashMap<>(trackingData);
            this.latestTrackingData.put("server_timestamp", LocalDateTime.now().toString());
            
            // 개별 차량 데이터 저장
            List<Map<String, Object>> vehicles = (List<Map<String, Object>>) trackingData.get("vehicles");
            if (vehicles != null) {
                for (Map<String, Object> vehicle : vehicles) {
                    String vehicleId = (String) vehicle.get("vehicle_id");
                    if (vehicleId != null) {
                        // 개별 차량 데이터에 서버 타임스탬프 추가
                        Map<String, Object> vehicleRecord = new HashMap<>(vehicle);
                        vehicleRecord.put("last_updated", LocalDateTime.now().toString());
                        
                        this.vehicleData.put(vehicleId, vehicleRecord);
                    }
                }
            }
            
            log.info("차량 추적 데이터 처리 완료: {} 대", trackingData.get("total_vehicles"));
            
        } catch (Exception e) {
            log.error("차량 추적 데이터 처리 중 오류", e);
            throw new RuntimeException("차량 추적 데이터 처리 실패", e);
        }
    }
    
    /**
     * 생산 통계 데이터 처리
     */
    public void processProductionStats(Map<String, Object> statsData) {
        try {
            this.latestProductionStats = new HashMap<>(statsData);
            this.latestProductionStats.put("server_timestamp", LocalDateTime.now().toString());
            
            log.info("생산 통계 데이터 처리 완료");
            
        } catch (Exception e) {
            log.error("생산 통계 데이터 처리 중 오류", e);
            throw new RuntimeException("생산 통계 데이터 처리 실패", e);
        }
    }
    
    /**
     * 현재 모든 차량 데이터 조회
     */
    public Map<String, Object> getCurrentVehicleData() {
        if (latestTrackingData.isEmpty()) {
            return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "total_vehicles", 0,
                "active_vehicles", 0,
                "vehicles", new ArrayList<>(),
                "station_sequence", new ArrayList<>(),
                "station_positions", new HashMap<>()
            );
        }
        
        return new HashMap<>(latestTrackingData);
    }
    
    /**
     * 특정 차량 상세 정보 조회
     */
    public Map<String, Object> getVehicleDetails(String vehicleId) {
        Map<String, Object> vehicle = vehicleData.get(vehicleId);
        
        if (vehicle == null) {
            return null;
        }
        
        // 추가 상세 정보 계산
        Map<String, Object> details = new HashMap<>(vehicle);
        
        // 진행률 계산
        Integer currentStationIndex = (Integer) vehicle.get("current_station_index");
        Integer totalStations = (Integer) vehicle.get("total_stations");
        
        if (currentStationIndex != null && totalStations != null) {
            double progressPercentage = ((double) currentStationIndex / totalStations) * 100;
            details.put("overall_progress", Math.round(progressPercentage * 10.0) / 10.0);
        }
        
        // 예상 완료 시간 계산 (간단한 추정)
        if (currentStationIndex != null && totalStations != null) {
            int remainingStations = totalStations - currentStationIndex;
            int estimatedMinutesRemaining = remainingStations * 5; // 스테이션당 5분 가정
            details.put("estimated_completion_minutes", estimatedMinutesRemaining);
        }
        
        return details;
    }
    
    /**
     * 특정 스테이션의 차량 목록 조회
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getVehiclesByStation(String stationId) {
        List<Map<String, Object>> stationVehicles = new ArrayList<>();
        
        for (Map<String, Object> vehicle : vehicleData.values()) {
            Map<String, Object> position = (Map<String, Object>) vehicle.get("position");
            
            if (position != null && stationId.equals(position.get("station_id"))) {
                stationVehicles.add(new HashMap<>(vehicle));
            }
        }
        
        return stationVehicles;
    }
    
    /**
     * 생산 통계 조회
     */
    public Map<String, Object> getProductionStatistics() {
        if (latestProductionStats.isEmpty()) {
            return calculateRealTimeStats();
        }
        
        // 실시간 계산 통계와 저장된 통계 결합
        Map<String, Object> realTimeStats = calculateRealTimeStats();
        Map<String, Object> combinedStats = new HashMap<>(latestProductionStats);
        combinedStats.putAll(realTimeStats);
        
        return combinedStats;
    }
    
    /**
     * 실시간 통계 계산
     */
    private Map<String, Object> calculateRealTimeStats() {
        int totalVehicles = vehicleData.size();
        long completedVehicles = vehicleData.values().stream()
            .mapToLong(vehicle -> "completed".equals(vehicle.get("status")) ? 1 : 0)
            .sum();
        
        long failedVehicles = vehicleData.values().stream()
            .mapToLong(vehicle -> "failed".equals(vehicle.get("status")) ? 1 : 0)
            .sum();
        
        long activeVehicles = vehicleData.values().stream()
            .mapToLong(vehicle -> {
                String status = (String) vehicle.get("status");
                return ("in_process".equals(status) || "waiting".equals(status) || "moving".equals(status)) ? 1 : 0;
            })
            .sum();
        
        double completionRate = totalVehicles > 0 ? 
            (double) completedVehicles / totalVehicles * 100 : 0.0;
        
        return Map.of(
            "real_time_total_vehicles", totalVehicles,
            "real_time_completed_vehicles", completedVehicles,
            "real_time_failed_vehicles", failedVehicles,
            "real_time_active_vehicles", activeVehicles,
            "real_time_completion_rate", Math.round(completionRate * 10.0) / 10.0,
            "last_calculated", LocalDateTime.now().toString()
        );
    }
    
    /**
     * 모든 차량 데이터 초기화 (테스트용)
     */
    public void clearAllData() {
        vehicleData.clear();
        latestTrackingData.clear();
        latestProductionStats.clear();
        log.info("모든 차량 데이터가 초기화되었습니다.");
    }
    
    /**
     * 현재 저장된 차량 수 조회
     */
    public int getCurrentVehicleCount() {
        return vehicleData.size();
    }
}