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
@Table(name = "conveyor_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConveyorStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;           // 데이터 전송 시각
    
    @Column(name = "command", nullable = false)
    private String command;                    // START/STOP/PAUSE/EMERGENCY_STOP
    
    @Column(name = "reason")
    private String reason;                     // 제어 사유
    
    @Column(name = "line_id", nullable = false)
    private Long lineId;
}
