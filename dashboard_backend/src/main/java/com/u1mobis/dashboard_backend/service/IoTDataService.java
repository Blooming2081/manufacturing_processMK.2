package com.u1mobis.dashboard_backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoTDataService {
    
    private final InfluxDB3Service influxDB3Service;
    
    public void processIoTData(Map<String, Object> iotData) {
        try {
            // 1. 로그 출력
            String stationId = (String) iotData.get("stationId");
            log.info("IoT 데이터 처리 시작 - Station: {}", stationId);
            
            // 2. InfluxDB에 저장 (시계열 데이터)
            saveToInfluxDB(iotData);
            
            log.info("IoT 데이터 처리 완료 - Station: {}", stationId);
            
        } catch (Exception e) {
            log.error("IoT 데이터 처리 중 오류 발생", e);
            throw new RuntimeException("IoT 데이터 처리 실패", e);
        }
    }
    
    private void saveToInfluxDB(Map<String, Object> iotData) {
        try {
            // 안전한 타입 변환
            String stationId = safeStringConvert(iotData.get("stationId"));
            String timestamp = safeStringConvert(iotData.get("timestamp"));
            
            // InfluxDB 3.x writeData 메서드에 맞는 파라미터 준비
            Map<String, String> tags = new java.util.HashMap<>();
            Map<String, Object> fields = new java.util.HashMap<>();
            
            // 태그 추가
            tags.put("station_id", stationId);
            if (iotData.containsKey("processType")) {
                tags.put("process_type", safeStringConvert(iotData.get("processType")));
            }
            if (iotData.containsKey("location")) {
                tags.put("location", safeStringConvert(iotData.get("location")));
            }
            
            // 필드 추가
            // 센서 데이터
            if (iotData.containsKey("sensors")) {
                Map<String, Object> sensors = (Map<String, Object>) iotData.get("sensors");
                for (Map.Entry<String, Object> entry : sensors.entrySet()) {
                    fields.put("sensor_" + entry.getKey(), entry.getValue());
                }
            }
            
            // 생산 데이터
            if (iotData.containsKey("production")) {
                Map<String, Object> production = (Map<String, Object>) iotData.get("production");
                for (Map.Entry<String, Object> entry : production.entrySet()) {
                    fields.put("production_" + entry.getKey(), entry.getValue());
                }
            }
            
            // 품질 데이터
            if (iotData.containsKey("quality")) {
                Map<String, Object> quality = (Map<String, Object>) iotData.get("quality");
                for (Map.Entry<String, Object> entry : quality.entrySet()) {
                    fields.put("quality_" + entry.getKey(), entry.getValue());
                }
            }
            
            // 기본 필드 추가 (필드가 비어있으면 안됨)
            if (fields.isEmpty()) {
                fields.put("value", 1.0);
            }
            
            // 타임스탬프 파싱
            java.time.Instant instant;
            if (timestamp != null) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(timestamp.replace("Z", ""));
                    instant = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (Exception e) {
                    instant = java.time.Instant.now();
                }
            } else {
                instant = java.time.Instant.now();
            }
            
            // InfluxDB에 쓰기
            influxDB3Service.writeData("IOT-sensor", tags, fields, instant);
            
            log.debug("InfluxDB 저장 완료 - Station: {}", stationId);
            
        } catch (Exception e) {
            log.error("InfluxDB 저장 중 오류 발생", e);
            // InfluxDB 오류는 전체 프로세스를 중단시키지 않음
        }
    }
    
    /**
     * 안전한 String 타입 변환
     */
    private String safeStringConvert(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}