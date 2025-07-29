package com.u1mobis.dashboard_backend.service;

import com.u1mobis.dashboard_backend.entity.Company;
import com.u1mobis.dashboard_backend.entity.EnvironmentSensor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendThresholdAlert(Company company, String alertType, String message, Object value) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "danger");
        alert.put("alertType", alertType);
        alert.put("message", message);
        alert.put("value", value);
        alert.put("timestamp", LocalDateTime.now().toString());
        alert.put("companyId", company.getCompanyId());
        alert.put("companyName", company.getCompanyName());

        // 해당 회사에만 알림 전송
        String destination = "/topic/alerts/" + company.getCompanyName();
        messagingTemplate.convertAndSend(destination, alert);
        
        log.warn("임계값 알림 전송 - 회사: {}, 타입: {}, 메시지: {}", 
                company.getCompanyName(), alertType, message);
    }

    public void sendEnvironmentAlert(Company company, EnvironmentSensor sensor) {
        if (sensor.getTemperature() > 35.0) {
            sendThresholdAlert(company, "TEMPERATURE", 
                    String.format("고온 경보: %.1f°C", sensor.getTemperature()),
                    sensor.getTemperature());
        }
        
        if (sensor.getHumidity() > 80.0) {
            sendThresholdAlert(company, "HUMIDITY", 
                    String.format("고습도 경보: %.1f%", sensor.getHumidity()),
                    sensor.getHumidity());
        }
        
        if (sensor.getAirQuality() > 300) {
            sendThresholdAlert(company, "AIR_QUALITY", 
                    String.format("공기질 경보: %d", sensor.getAirQuality()),
                    sensor.getAirQuality());
        }
    }

    public void sendProductionAlert(Company company, String productId, String alertType, String message) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "warning");
        alert.put("alertType", alertType);
        alert.put("message", message);
        alert.put("productId", productId);
        alert.put("timestamp", LocalDateTime.now().toString());
        alert.put("companyId", company.getCompanyId());
        alert.put("companyName", company.getCompanyName());

        String destination = "/topic/alerts/" + company.getCompanyName();
        messagingTemplate.convertAndSend(destination, alert);
        
        log.warn("생산 알림 전송 - 회사: {}, 제품: {}, 타입: {}, 메시지: {}", 
                company.getCompanyName(), productId, alertType, message);
    }
}