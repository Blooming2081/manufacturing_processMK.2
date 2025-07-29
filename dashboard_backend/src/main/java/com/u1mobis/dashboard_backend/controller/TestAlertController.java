package com.u1mobis.dashboard_backend.controller;

import com.u1mobis.dashboard_backend.entity.Company;
import com.u1mobis.dashboard_backend.entity.EnvironmentSensor;
import com.u1mobis.dashboard_backend.repository.CompanyRepository;
import com.u1mobis.dashboard_backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestAlertController {

    private final AlertService alertService;
    private final CompanyRepository companyRepository;

    @PostMapping("/alert/temperature/{companyCode}")
    public ResponseEntity<Map<String, Object>> testTemperatureAlert(
            @PathVariable String companyCode,
            @RequestParam(defaultValue = "40.0") Double temperature) {
        
        Optional<Company> companyOpt = companyRepository.findByCompanyCode(companyCode);
        if (companyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "회사를 찾을 수 없습니다: " + companyCode));
        }

        Company company = companyOpt.get();
        EnvironmentSensor sensor = EnvironmentSensor.builder()
                .temperature(temperature)
                .humidity(50.0)
                .airQuality(200)
                .timestamp(LocalDateTime.now())
                .build();

        alertService.sendEnvironmentAlert(company, sensor);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "온도 알림이 전송되었습니다");
        response.put("companyCode", companyCode);
        response.put("temperature", temperature);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/alert/humidity/{companyCode}")
    public ResponseEntity<Map<String, Object>> testHumidityAlert(
            @PathVariable String companyCode,
            @RequestParam(defaultValue = "85.0") Double humidity) {
        
        Optional<Company> companyOpt = companyRepository.findByCompanyCode(companyCode);
        if (companyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "회사를 찾을 수 없습니다: " + companyCode));
        }

        Company company = companyOpt.get();
        EnvironmentSensor sensor = EnvironmentSensor.builder()
                .temperature(25.0)
                .humidity(humidity)
                .airQuality(200)
                .timestamp(LocalDateTime.now())
                .build();

        alertService.sendEnvironmentAlert(company, sensor);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "습도 알림이 전송되었습니다");
        response.put("companyCode", companyCode);
        response.put("humidity", humidity);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/alert/air-quality/{companyCode}")
    public ResponseEntity<Map<String, Object>> testAirQualityAlert(
            @PathVariable String companyCode,
            @RequestParam(defaultValue = "350") Integer airQuality) {
        
        Optional<Company> companyOpt = companyRepository.findByCompanyCode(companyCode);
        if (companyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "회사를 찾을 수 없습니다: " + companyCode));
        }

        Company company = companyOpt.get();
        EnvironmentSensor sensor = EnvironmentSensor.builder()
                .temperature(25.0)
                .humidity(50.0)
                .airQuality(airQuality)
                .timestamp(LocalDateTime.now())
                .build();

        alertService.sendEnvironmentAlert(company, sensor);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "공기질 알림이 전송되었습니다");
        response.put("companyCode", companyCode);
        response.put("airQuality", airQuality);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/alert/production/{companyCode}")
    public ResponseEntity<Map<String, Object>> testProductionAlert(
            @PathVariable String companyCode,
            @RequestParam String productId,
            @RequestParam(defaultValue = "DELAY") String alertType,
            @RequestParam(defaultValue = "생산 지연이 발생했습니다") String message) {
        
        Optional<Company> companyOpt = companyRepository.findByCompanyCode(companyCode);
        if (companyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "회사를 찾을 수 없습니다: " + companyCode));
        }

        Company company = companyOpt.get();
        alertService.sendProductionAlert(company, productId, alertType, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "생산 알림이 전송되었습니다");
        response.put("companyCode", companyCode);
        response.put("productId", productId);
        response.put("alertType", alertType);

        return ResponseEntity.ok(response);
    }
}