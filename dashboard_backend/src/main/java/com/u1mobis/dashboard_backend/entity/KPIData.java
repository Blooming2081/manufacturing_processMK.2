package com.u1mobis.dashboard_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kpi_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KPIData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kpi_id")
    private Long kpiId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;           // 데이터 전송 시각
    
    @Column(name = "planned_time")
    private Integer plannedTime;               // 계획 운전 시간 (분)
    
    @Column(name = "downtime")
    private Integer downtime;                  // 설비 정지 시간 (분)
    
    @Column(name = "target_cycle_time")
    private Double targetCycleTime;            // 목표 사이클 타임 (초)
    
    @Column(name = "good_count")
    private Integer goodCount;                 // 양품 수량
    
    @Column(name = "total_count")
    private Integer totalCount;                // 전체 생산 수량
    
    @Column(name = "first_time_pass_count")
    private Integer firstTimePassCount;        // 일발 통과 수량 (FTY 계산용)
    
    @Column(name = "on_time_delivery_count")
    private Integer onTimeDeliveryCount;       // 정시 납기 수량 (OTD 계산용)
    
    // 계산된 KPI 값들 (캐시용)
    @Column(name = "calculated_oee")
    private Double calculatedOEE;              // 계산된 OEE 값
    
    @Column(name = "calculated_fty")
    private Double calculatedFTY;              // 계산된 FTY 값
    
    @Column(name = "calculated_otd")
    private Double calculatedOTD;              // 계산된 OTD 값

    // 외래키 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = true)
    private Company company;

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    // OEE 필드 추가 (PDF 스키마에 맞춤)
    @Column(name = "oee")
    private Double oee;

    // 편의 메서드: companyId 조회
    public Long getCompanyId() {
        return company != null ? company.getCompanyId() : null;
    }

    // 비즈니스 로직용 생성자
    public KPIData(Company company, Double oee) {
        this.company = company;
        this.oee = oee;
        this.timestamp = LocalDateTime.now();
    }

    // 기존 코드 호환성을 위한 생성자
    public KPIData(Long companyId, Double oee) {
        this.oee = oee;
        this.timestamp = LocalDateTime.now();
        // company는 별도로 설정 필요
    }
}