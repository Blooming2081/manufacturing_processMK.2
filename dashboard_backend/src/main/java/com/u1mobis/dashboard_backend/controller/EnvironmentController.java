package com.u1mobis.dashboard_backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.u1mobis.dashboard_backend.entity.EnvironmentSensor;
import com.u1mobis.dashboard_backend.service.EnvironmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/{companyName}/environment")
@RequiredArgsConstructor
@Slf4j
public class EnvironmentController {

    private final EnvironmentService environmentService;

    /**
     * 현재 환경 데이터 조회 (리액트용)
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentEnvironment(@PathVariable String companyName) {
        log.info("환경 데이터 요청 - 회사: {}", companyName);
        try {
            Map<String, Object> environmentData = environmentService.getCurrentEnvironment();
            return ResponseEntity.ok(environmentData);

        } catch (Exception e) {
            log.error("환경 데이터 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "환경 데이터 조회 실패"));
        }
    }

    /**
     * 환경 데이터 입력 (테스트용)
     */
    @PostMapping("/input")
    public ResponseEntity<Map<String, Object>> inputEnvironmentData(@PathVariable String companyName, @RequestBody Map<String, Object> envData) {
        log.info("환경 데이터 입력 요청 - 회사: {}", companyName);
        try {
            log.info("환경 데이터 수신: {}", envData);

            Double temperature = convertToDouble(envData.get("temperature"));
            Double humidity = convertToDouble(envData.get("humidity"));
            Integer airQuality = convertToInteger(envData.get("air_quality"));

            EnvironmentSensor result = environmentService.saveEnvironmentData(temperature, humidity, airQuality);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "temperature", result.getTemperature(),
                    "humidity", result.getHumidity(),
                    "air_quality", result.getAirQuality(),
                    "timestamp", result.getTimestamp().toString()));

        } catch (Exception e) {
            log.error("환경 데이터 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    // 헬퍼 메서드들
    private Double convertToDouble(Object value) {
        if (value == null)
            return 0.0;
        if (value instanceof Double)
            return (Double) value;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private Integer convertToInteger(Object value) {
        if (value == null)
            return 0;
        if (value instanceof Integer)
            return (Integer) value;
        if (value instanceof Number)
            return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
}
