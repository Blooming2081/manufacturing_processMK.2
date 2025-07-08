package com.u1mobis.dashboard_backend.controller;

import com.u1mobis.dashboard_backend.dto.IoTDataDto;
import com.u1mobis.dashboard_backend.service.IoTDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/iot")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
@Slf4j
public class IoTDataController {
    
    private final IoTDataService iotDataService;
    
    /**
     * Data Collector에서 IoT 데이터 수신 및 InfluxDB 저장
     */
    @PostMapping("/data")
    public ResponseEntity<Map<String, String>> receiveIoTData(@RequestBody IoTDataDto iotData) {
        try {
            log.info("IoT 데이터 수신: {}", iotData.getStationId());
            
            // IoTDataService에서 InfluxDB 저장 처리
            iotDataService.processIoTData(iotData);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "IoT 데이터가 성공적으로 처리되었습니다."
            ));
            
        } catch (Exception e) {
            log.error("IoT 데이터 처리 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "IoT 데이터 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 특정 스테이션의 최근 센서 데이터 조회
     */
    @GetMapping("/sensors/{stationId}")
    public ResponseEntity<Map<String, Object>> getSensorData(
            @PathVariable String stationId,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            log.info("센서 데이터 조회 요청: Station={}, Limit={}", stationId, limit);
            
            // TODO: InfluxDB에서 센서 데이터 조회 로직 구현
            // influxDb3Service.getRecentSensorData(stationId, limit) 활용
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "stationId", stationId,
                "message", "센서 데이터 조회 기능은 아직 구현 중입니다."
            ));
            
        } catch (Exception e) {
            log.error("센서 데이터 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "센서 데이터 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}