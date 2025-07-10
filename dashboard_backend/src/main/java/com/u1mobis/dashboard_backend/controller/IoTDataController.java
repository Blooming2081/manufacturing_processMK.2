package com.u1mobis.dashboard_backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.u1mobis.dashboard_backend.service.IoTDataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
@Slf4j
public class IoTDataController {
    
    private final IoTDataService iotDataService;
    
    /**
     * Data Collector에서 원시 IoT 데이터 수신
     */
    @PostMapping("/iot-data")
    public ResponseEntity<Map<String, String>> receiveIoTData(@RequestBody Map<String, Object> iotData) {
        try {
            log.info("IoT 데이터 수신: {}", iotData.get("stationId"));
            
            iotDataService.processIoTData(iotData);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "IoT 데이터가 성공적으로 처리되었습니다.",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("IoT 데이터 처리 중 오류 발생", e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "IoT 데이터 처리 중 오류가 발생했습니다: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
}