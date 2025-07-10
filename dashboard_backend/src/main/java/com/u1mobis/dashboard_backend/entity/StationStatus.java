package com.u1mobis.dashboard_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "station_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;
    
    @Column(name = "station_name", length = 100)
    private String stationName;
    
    @Column(name = "status", length = 50)
    private String status; // RUNNING, IDLE, ERROR, MAINTENANCE
    
    @Column(name = "current_operation", length = 100)
    private String currentOperation;
    
    @Column(name = "cycle_time")
    private Double cycleTime;
    
    @Column(name = "target_cycle_time")
    private Double targetCycleTime;
    
    @Column(name = "production_count")
    private Integer productionCount;
    
    @Column(name = "progress")
    private Double progress;
    
    @Column(name = "efficiency")
    private Double efficiency;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public boolean isRunning() {
        return "RUNNING".equals(status);
    }
    
    public boolean hasError() {
        return "ERROR".equals(status);
    }
    
    public boolean isEfficient() {
        return efficiency != null && efficiency >= 80.0;
    }
}