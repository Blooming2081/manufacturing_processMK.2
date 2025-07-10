package com.u1mobis.dashboard_backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.u1mobis.dashboard_backend.entity.StationStatus;
import com.u1mobis.dashboard_backend.repository.StationStatusRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StationStatusService {
    
    @Autowired
    private final StationStatusRepository stationStatusRepository;
    
    /**
     * MQTT 상태 데이터 처리 및 저장
     */
    @Transactional
    public StationStatus processStatusData(Map<String, Object> statusData) {
        try {
            String stationId = (String) statusData.get("station_id");
            
            // StationStatus 엔티티 생성
            StationStatus stationStatus = createStationStatus(statusData);
            
            // 데이터베이스에 저장
            StationStatus savedStatus = stationStatusRepository.save(stationStatus);
            
            log.info("스테이션 상태 저장 완료 - Station: {}, Status: {}", 
                stationId, savedStatus.getStatus());
            
            return savedStatus;
            
        } catch (Exception e) {
            log.error("상태 데이터 처리 실패: ", e);
            throw new RuntimeException("상태 데이터 처리 실패", e);
        }
    }
    
    /**
     * StationStatus 엔티티 생성
     */
    private StationStatus createStationStatus(Map<String, Object> statusData) {
        StationStatus.StationStatusBuilder builder = StationStatus.builder();
        
        // 필수 필드
        builder.stationId((String) statusData.get("station_id"));
        
        // 선택적 필드들
        if (statusData.containsKey("station_name")) {
            builder.stationName((String) statusData.get("station_name"));
        }
        
        if (statusData.containsKey("station_status")) {
            builder.status((String) statusData.get("station_status"));
        } else if (statusData.containsKey("status")) {
            builder.status((String) statusData.get("status"));
        }
        
        if (statusData.containsKey("current_operation")) {
            builder.currentOperation((String) statusData.get("current_operation"));
        }
        
        if (statusData.containsKey("cycle_time")) {
            Object cycleTime = statusData.get("cycle_time");
            if (cycleTime instanceof Number) {
                builder.cycleTime(((Number) cycleTime).doubleValue());
            }
        }
        
        if (statusData.containsKey("target_cycle_time")) {
            Object targetCycleTime = statusData.get("target_cycle_time");
            if (targetCycleTime instanceof Number) {
                builder.targetCycleTime(((Number) targetCycleTime).doubleValue());
            }
        }
        
        if (statusData.containsKey("production_count")) {
            Object productionCount = statusData.get("production_count");
            if (productionCount instanceof Number) {
                builder.productionCount(((Number) productionCount).intValue());
            }
        }
        
        if (statusData.containsKey("progress")) {
            Object progress = statusData.get("progress");
            if (progress instanceof Number) {
                builder.progress(((Number) progress).doubleValue());
            }
        }
        
        if (statusData.containsKey("efficiency")) {
            Object efficiency = statusData.get("efficiency");
            if (efficiency instanceof Number) {
                builder.efficiency(((Number) efficiency).doubleValue());
            }
        }
        
        // 타임스탬프 처리
        if (statusData.containsKey("timestamp")) {
            String timestampStr = (String) statusData.get("timestamp");
            try {
                // ISO 8601 형식으로 파싱
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
                builder.timestamp(timestamp);
            } catch (Exception e) {
                log.warn("타임스탬프 파싱 실패, 현재 시간 사용: {}", timestampStr);
                builder.timestamp(LocalDateTime.now());
            }
        } else {
            builder.timestamp(LocalDateTime.now());
        }
        
        return builder.build();
    }
    
    /**
     * 특정 스테이션의 최신 상태 조회
     */
    public StationStatus getLatestStatus(String stationId) {
        return stationStatusRepository.findTopByStationIdOrderByTimestampDesc(stationId).orElse(null);
    }
    
    /**
     * 모든 스테이션의 최신 상태 조회
     */
    public List<StationStatus> getAllLatestStatus() {
        return stationStatusRepository.findLatestStatusForAllStations();
    }
    
    /**
     * 상태별 스테이션 조회
     */
    public List<StationStatus> getStationsByStatus(String status) {
        return stationStatusRepository.findByStatus(status);
    }
    
    /**
     * 실행 중인 스테이션 조회
     */
    public List<StationStatus> getRunningStations() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return stationStatusRepository.findRunningStations(fiveMinutesAgo);
    }
    
    /**
     * 스테이션 통계 정보
     */
    public Map<String, Object> getStationStatistics(String stationId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            StationStatus latestStatus = getLatestStatus(stationId);
            
            if (latestStatus != null) {
                stats.put("stationId", latestStatus.getStationId());
                stats.put("stationName", latestStatus.getStationName());
                stats.put("currentStatus", latestStatus.getStatus());
                stats.put("isRunning", latestStatus.isRunning());
                stats.put("hasError", latestStatus.hasError());
                stats.put("isEfficient", latestStatus.isEfficient());
                stats.put("lastUpdate", latestStatus.getTimestamp());
                stats.put("productionCount", latestStatus.getProductionCount());
                stats.put("efficiency", latestStatus.getEfficiency());
                stats.put("progress", latestStatus.getProgress());
                
                // 최근 24시간 데이터 수
                LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
                List<StationStatus> recentData = stationStatusRepository.findByStationIdAndTimestampBetween(
                    stationId, yesterday, LocalDateTime.now());
                stats.put("recentDataCount", recentData.size());
            } else {
                stats.put("stationId", stationId);
                stats.put("message", "데이터가 없습니다");
            }
            
        } catch (Exception e) {
            log.error("스테이션 통계 생성 실패: ", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 시스템 상태 확인
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 전체 레코드 수
            long totalRecords = stationStatusRepository.count();
            health.put("totalRecords", totalRecords);
            
            // 최근 5분간 활성 스테이션 수
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            List<StationStatus> recentData = stationStatusRepository.findByTimestampAfter(fiveMinutesAgo);
            health.put("activeStations", recentData.size());
            
            // 실행 중인 스테이션 수
            List<StationStatus> runningStations = getRunningStations();
            health.put("runningStations", runningStations.size());
            
            // 모든 스테이션의 최신 상태
            List<StationStatus> allLatest = getAllLatestStatus();
            health.put("totalStations", allLatest.size());
            
            long errorStations = allLatest.stream()
                .mapToLong(s -> s.hasError() ? 1 : 0)
                .sum();
            health.put("errorStations", errorStations);
            
            health.put("status", "healthy");
            health.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            health.put("status", "error");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
        }
        
        return health;
    }
    
    /**
     * 데이터베이스 연결 테스트
     */
    public boolean testDatabaseConnection() {
        try {
            stationStatusRepository.count();
            return true;
        } catch (Exception e) {
            log.error("데이터베이스 연결 테스트 실패: ", e);
            return false;
        }
    }
    
    /**
     * 전체 상태 레코드 수 조회
     */
    public long getTotalStatusCount() {
        return stationStatusRepository.count();
    }
}