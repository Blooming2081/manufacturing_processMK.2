package com.u1mobis.dashboard_backend.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.u1mobis.dashboard_backend.entity.Company;
import com.u1mobis.dashboard_backend.entity.EnvironmentSensor;
import com.u1mobis.dashboard_backend.repository.CompanyRepository;
import com.u1mobis.dashboard_backend.repository.EnvironmentSensorRepository;
import com.u1mobis.dashboard_backend.security.CustomUserDetailsService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentService {

    private final EnvironmentSensorRepository environmentSensorRepository;
    private final CompanyRepository companyRepository;
    private final AlertService alertService;

    // 환경 데이터 저장 (MQTT에서 호출) - 회사별
    public EnvironmentSensor saveEnvironmentData(Long companyId, Double temperature, Double humidity, Integer airQuality) {
        EnvironmentSensor sensor = EnvironmentSensor.builder()
                .timestamp(LocalDateTime.now())
                .temperature(temperature)
                .humidity(humidity)
                .airQuality(airQuality)
                .build();

        // 알람 체크 - 회사별
        checkEnvironmentAlarms(companyId, sensor);

        return environmentSensorRepository.save(sensor);
    }

    // 환경 데이터 저장 (기존 호환성용) - 현재 로그인된 사용자의 회사 사용
    public EnvironmentSensor saveEnvironmentData(Double temperature, Double humidity, Integer airQuality) {
        Long companyId = getCurrentUserCompanyId();
        return saveEnvironmentData(companyId, temperature, humidity, airQuality);
    }

    // 환경 데이터 저장 (MQTT용) - 회사명으로 조회
    public EnvironmentSensor saveEnvironmentData(String companyName, Double temperature, Double humidity, Integer airQuality) {
        Long companyId = getCompanyIdByName(companyName);
        return saveEnvironmentData(companyId, temperature, humidity, airQuality);
    }

    // 현재 로그인된 사용자의 회사 ID 가져오기
    private Long getCurrentUserCompanyId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal) {
                CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                    (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
                return userPrincipal.getCompanyId();
            }
        } catch (Exception e) {
            log.warn("현재 사용자의 회사 정보를 가져올 수 없습니다: {}", e.getMessage());
        }
        return 1L; // 기본값
    }

    // 환경 알람 체크 - 회사별 웹소켓 알림
    private void checkEnvironmentAlarms(Long companyId, EnvironmentSensor sensor) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isPresent()) {
            Company company = companyOpt.get();
            alertService.sendEnvironmentAlert(company, sensor);
        } else {
            // 회사를 찾을 수 없는 경우도 로그 출력
            if (sensor.getTemperature() > 35.0) {
                log.warn("고온 경보 (회사 ID: {}): {}°C", companyId, sensor.getTemperature());
            }
            if (sensor.getHumidity() > 80.0) {
                log.warn("고습도 경보 (회사 ID: {}): {}%", companyId, sensor.getHumidity());
            }
            if (sensor.getAirQuality() > 300) {
                log.warn("공기질 경보 (회사 ID: {}): {}", companyId, sensor.getAirQuality());
            }
        }
    }

    // 회사명으로 회사 ID 조회
    private Long getCompanyIdByName(String companyName) {
        Optional<Company> company = companyRepository.findByCompanyName(companyName);
        if (company.isPresent()) {
            return company.get().getCompanyId();
        } else {
            log.warn("회사를 찾을 수 없습니다: {}", companyName);
            throw new RuntimeException("회사를 찾을 수 없습니다: " + companyName);
        }
    }

    // 환경 알람 체크 (기존 호환성용)
    private void checkEnvironmentAlarms(EnvironmentSensor sensor) {
        checkEnvironmentAlarms(1L, sensor); // 기본 회사 ID
    }

    // 최신 환경 데이터 조회
    public Map<String, Object> getCurrentEnvironment() {
        Optional<EnvironmentSensor> latest = environmentSensorRepository.findTopByOrderByTimestampDesc();

        if (latest.isPresent()) {
            EnvironmentSensor env = latest.get();
            return Map.of(
                    "temperature", env.getTemperature(),
                    "humidity", env.getHumidity(),
                    "air_quality", env.getAirQuality(),
                    "timestamp", env.getTimestamp().toString(), // 문자열로 변환
                    "status", getEnvironmentStatus(env));
        }

        return Map.of("status", "NO_DATA");
    }

    private String getEnvironmentStatus(EnvironmentSensor env) {
        if (env.getTemperature() > 35 || env.getHumidity() > 80 || env.getAirQuality() > 300) {
            return "WARNING";
        }
        return "NORMAL";
    }
}