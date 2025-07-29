package com.u1mobis.dashboard_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "current_production")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentProduction {
    @Id
    @Column(name = "product_id")
    private String productId;                  // 차량 고유 ID (Primary Key)

    @Column(name = "product_color")
    private String productColor;                  // 차량 색깔
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;           // 생산 시작 시각
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;             // 납기 예정일
    
    @Column(name = "rework_count")
    @Builder.Default
    private Integer reworkCount = 0;           // 재작업 횟수
    
    @Column(name = "current_station")
    private String currentStation;             // 현재 위치한 스테이션
    
    @Column(name = "status")
    private String status;                     // PROCESSING/COMPLETED/FAILED
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "line_id", nullable = false)
    private Long lineId;
}
