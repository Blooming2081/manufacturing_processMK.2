package com.u1mobis.dashboard_backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.u1mobis.dashboard_backend.service.ConveyorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/{companyName}/conveyor")
@RequiredArgsConstructor
@Slf4j
public class ConveyorController {
    
    private final ConveyorService conveyorService;
    
    /**
     * 현재 컨베이어 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCurrentStatus(@PathVariable String companyName) {
        log.info("컨베이어 상태 요청 - 회사: {}", companyName);
        try {
            Map<String, Object> conveyorStatus = conveyorService.getCurrentConveyorStatus();
            return ResponseEntity.ok(conveyorStatus);
            
        } catch (Exception e) {
            log.error("컨베이어 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "컨베이어 상태 조회 실패"
            ));
        }
    }
}