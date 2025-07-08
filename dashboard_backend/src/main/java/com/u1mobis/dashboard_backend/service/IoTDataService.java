package com.u1mobis.dashboard_backend.service;

import com.u1mobis.dashboard_backend.dto.IoTDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoTDataService {
    
    private final InfluxDB3Service influxDb3Service;
    
    /**
     * IoT 데이터 처리 및 InfluxDB 저장
     */
    public void processIoTData(IoTDataDto iotData) {
        try {
            log.info("IoT 데이터 처리 시작 - Station: {}", iotData.getStationId());
            
            // InfluxDB에 센서 데이터 저장
            saveSensorDataToInfluxDB(iotData);
            
            // InfluxDB에 생산 데이터 저장  
            saveProductionDataToInfluxDB(iotData);
            
            log.info("IoT 데이터 처리 완료 - Station: {}", iotData.getStationId());
            
        } catch (Exception e) {
            log.error("IoT 데이터 처리 실패 - Station: {}, Error: {}", 
                     iotData.getStationId(), e.getMessage());
        }
    }
    
    /**
     * 센서 데이터를 InfluxDB에 저장
     */
    private void saveSensorDataToInfluxDB(IoTDataDto iotData) {
        try {
            if (iotData.getSensors() == null || iotData.getSensors().isEmpty()) {
                return;
            }
            
            // 태그 설정 (인덱스 역할)
            Map<String, String> tags = new HashMap<>();
            tags.put("station_id", iotData.getStationId());
            tags.put("process_type", iotData.getProcessType());
            tags.put("location", iotData.getLocation());
            
            // 필드 설정 (실제 데이터 값)
            Map<String, Object> fields = new HashMap<>();
            
            // 센서 데이터 추가
            iotData.getSensors().forEach((key, value) -> {
                if (value instanceof Number) {
                    fields.put("sensor_" + key, value);
                } else {
                    fields.put("sensor_" + key + "_str", value.toString());
                }
            });
            
            // 타임스탬프 파싱
            Instant timestamp = parseTimestamp(iotData.getTimestamp());
            
            // InfluxDB에 저장
            influxDb3Service.writeData("iot_sensors", tags, fields, timestamp)
                .subscribe(
                    success -> {
                        if (success) {
                            log.debug("✅ 센서 데이터 InfluxDB 저장 성공: {}", iotData.getStationId());
                        } else {
                            log.warn("⚠️ 센서 데이터 InfluxDB 저장 실패: {}", iotData.getStationId());
                        }
                    },
                    error -> log.error("❌ 센서 데이터 InfluxDB 저장 오류: {}", error.getMessage())
                );
                
        } catch (Exception e) {
            log.error("센서 데이터 InfluxDB 저장 처리 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 생산 데이터를 InfluxDB에 저장
     */
    private void saveProductionDataToInfluxDB(IoTDataDto iotData) {
        try {
            if (iotData.getProduction() == null || iotData.getProduction().isEmpty()) {
                return;
            }
            
            // 태그 설정
            Map<String, String> tags = new HashMap<>();
            tags.put("station_id", iotData.getStationId());
            tags.put("process_type", iotData.getProcessType());
            tags.put("location", iotData.getLocation());
            
            // 생산 데이터 필드 설정
            Map<String, Object> fields = new HashMap<>();
            
            iotData.getProduction().forEach((key, value) -> {
                if (value instanceof Number) {
                    fields.put("prod_" + key, value);
                } else {
                    fields.put("prod_" + key + "_str", value.toString());
                }
            });
            
            // 품질 데이터도 함께 저장
            if (iotData.getQuality() != null && !iotData.getQuality().isEmpty()) {
                iotData.getQuality().forEach((key, value) -> {
                    if (value instanceof Number) {
                        fields.put("quality_" + key, value);
                    } else {
                        fields.put("quality_" + key + "_str", value.toString());
                    }
                });
            }
            
            Instant timestamp = parseTimestamp(iotData.getTimestamp());
            
            // InfluxDB에 저장
            influxDb3Service.writeData("iot_production", tags, fields, timestamp)
                .subscribe(
                    success -> {
                        if (success) {
                            log.debug("✅ 생산 데이터 InfluxDB 저장 성공: {}", iotData.getStationId());
                        } else {
                            log.warn("⚠️ 생산 데이터 InfluxDB 저장 실패: {}", iotData.getStationId());
                        }
                    },
                    error -> log.error("❌ 생산 데이터 InfluxDB 저장 오류: {}", error.getMessage())
                );
                
        } catch (Exception e) {
            log.error("생산 데이터 InfluxDB 저장 처리 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 타임스탬프 문자열을 Instant로 변환
     */
    private Instant parseTimestamp(String timestampStr) {
        try {
            if (timestampStr == null || timestampStr.isEmpty()) {
                return Instant.now();
            }
            return Instant.parse(timestampStr);
        } catch (Exception e) {
            log.warn("타임스탬프 파싱 실패, 현재 시간 사용: {}", timestampStr);
            return Instant.now();
        }
    }
}