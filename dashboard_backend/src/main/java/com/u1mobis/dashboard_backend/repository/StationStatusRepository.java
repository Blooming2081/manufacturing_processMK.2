package com.u1mobis.dashboard_backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.u1mobis.dashboard_backend.entity.StationStatus;

@Repository
public interface StationStatusRepository extends JpaRepository<StationStatus, Long> {
    
    // 스테이션별 최신 상태 조회
    Optional<StationStatus> findTopByStationIdOrderByTimestampDesc(String stationId);
    
    // 상태별 스테이션 조회
    List<StationStatus> findByStatus(String status);
    
    // 특정 시간 이후의 데이터 조회
    List<StationStatus> findByTimestampAfter(LocalDateTime timestamp);
    
    // 스테이션별 시간 범위 데이터 조회
    List<StationStatus> findByStationIdAndTimestampBetween(
        String stationId, LocalDateTime start, LocalDateTime end);
    
    // 모든 스테이션의 최신 상태 조회
    @Query("SELECT s FROM StationStatus s WHERE s.timestamp = " +
           "(SELECT MAX(s2.timestamp) FROM StationStatus s2 WHERE s2.stationId = s.stationId)")
    List<StationStatus> findLatestStatusForAllStations();
    
    // 실행 중인 스테이션 조회
    @Query("SELECT s FROM StationStatus s WHERE s.status = 'RUNNING' AND s.timestamp >= :since")
    List<StationStatus> findRunningStations(@Param("since") LocalDateTime since);
    
    // 효율성 기준 조회
    @Query("SELECT s FROM StationStatus s WHERE s.efficiency >= :minEfficiency AND s.timestamp >= :since")
    List<StationStatus> findEfficientStations(@Param("minEfficiency") Double minEfficiency, @Param("since") LocalDateTime since);
}