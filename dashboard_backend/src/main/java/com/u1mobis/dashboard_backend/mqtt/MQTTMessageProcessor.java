package com.u1mobis.dashboard_backend.mqtt;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.u1mobis.dashboard_backend.entity.Company;
import com.u1mobis.dashboard_backend.repository.CompanyRepository;
import com.u1mobis.dashboard_backend.service.ConveyorService;
import com.u1mobis.dashboard_backend.service.EnvironmentService;
import com.u1mobis.dashboard_backend.service.KPICalculationService;
import com.u1mobis.dashboard_backend.service.ProductionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MQTTMessageProcessor {
    
    private final ProductionService productionService;
    private final KPICalculationService kpiCalculationService;
    private final EnvironmentService environmentService;
    private final ConveyorService conveyorService;
    private final CompanyRepository companyRepository;
    
    // MQTT 메시지 처리 (가상의 메서드 - 실제로는 MQTT 라이브러리 사용)
    public void processMQTTMessage(String topic, String payload) {
        try {
            log.info("=== MQTT 메시지 수신 시작 ===");
            log.info("토픽: {}", topic);
            log.info("페이로드: {}", payload);
            
            // 토픽에서 회사명 추출: factory/{companyName}/...
            String companyName = extractCompanyNameFromTopic(topic);
            if (companyName == null) {
                log.warn("토픽에서 회사명을 추출할 수 없습니다: {}", topic);
                return;
            }
            log.info("추출된 회사명: {}", companyName);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode data = mapper.readTree(payload);
            
            
            if (topic.contains("/production/started")) {
                log.info("생산 시작 처리 시작 - 회사: {}", companyName);
                
                // 토픽에서 lineId 추출
                Long lineId = extractLineIdFromTopic(topic);
                
                // 생산 시작 처리
                productionService.startProduction(
                    companyName,
                    lineId,
                    data.get("product_id").asText(),
                    data.get("target_quantity").asInt(),
                    LocalDateTime.parse(data.get("due_date").asText())
                );
                log.info("생산 시작 처리 완료 - 회사: {}, 라인: {}", companyName, lineId);
            }
            else if (topic.contains("/production/completed")) {
                log.info("생산 완료 처리 시작 - 회사: {}", companyName);
                
                // 토픽에서 lineId 추출
                Long lineId = extractLineIdFromTopic(topic);
                
                // 생산 완료 처리
                productionService.completeProduction(
                    companyName,
                    lineId,
                    data.get("product_id").asText(),
                    data.get("cycle_time").asDouble(),
                    data.get("quality").asText(),
                    LocalDateTime.parse(data.get("due_date").asText())
                );
                log.info("생산 완료 처리 완료 - 회사: {}, 라인: {}", companyName, lineId);
            }
            else if (topic.contains("/operations")) {
                log.info("KPI 데이터 처리 시작 - 회사: {}", companyName);
                
                // 토픽에서 lineId 추출 (factory/ms/b/operations -> lineId 2L)
                Long lineId = extractLineIdFromTopic(topic);
                
                // KPI 데이터 처리
                kpiCalculationService.processKPIData(
                    companyName,
                    lineId,
                    data.get("planned_time").asInt(),
                    data.get("downtime").asInt(),
                    data.get("target_cycle_time").asDouble(),
                    data.get("good_count").asInt(),
                    data.get("total_count").asInt(),
                    data.get("first_time_pass_count").asInt(),
                    data.get("on_time_delivery_count").asInt()
                );
                log.info("KPI 데이터 처리 완료 - 회사: {}", companyName);
            }
            else if (topic.contains("/environment")) {
                // 환경 데이터 처리
                environmentService.saveEnvironmentData(
                    companyName,
                    data.get("temperature").asDouble(),
                    data.get("humidity").asDouble(),
                    data.get("air_quality").asInt()
                );
            }
            else if (topic.contains("/conveyor")) {
                log.info("컨베이어 데이터 처리 시작 - 회사: {}", companyName);
                
                // 토픽에서 lineId 추출
                Long lineId = extractLineIdFromTopic(topic);
                
                // 컨베이어 제어 처리
                conveyorService.saveConveyorStatus(
                    companyName,
                    lineId,
                    data.get("command").asText(),
                    data.get("reason").asText()
                );
                log.info("컨베이어 상태 저장 완료 - 회사: {}, 라인: {}, 명령: {}", companyName, lineId, data.get("command").asText());
            }
            else {
                log.warn("처리되지 않은 토픽: {}", topic);
            }
            
        } catch (Exception e) {
            log.error("MQTT 메시지 처리 실패 - topic: {}, error: {}", topic, e.getMessage());
            log.error("에러 스택 트레이스:", e);
        }
    }
    
    /**
     * 토픽에서 회사 코드를 추출합니다.
     * 토픽 형식: factory/{companyCode}/...
     * companyCode를 통해 실제 회사명을 조회합니다.
     */
    private String extractCompanyNameFromTopic(String topic) {
        try {
            String[] parts = topic.split("/");
            log.info("토픽 파싱 결과: {}", String.join(", ", parts));
            if (parts.length >= 2 && "factory".equals(parts[0])) {
                String companyCode = parts[1];  // companyCode 추출
                log.info("추출된 회사 코드: '{}'", companyCode);
                
                // companyCode로 실제 회사명 조회
                return getCompanyNameByCode(companyCode);
            }
            log.warn("토픽 형식이 올바르지 않음: {}", topic);
            return null;
        } catch (Exception e) {
            log.error("토픽 파싱 실패: {}", topic, e);
            return null;
        }
    }
    
    /**
     * 회사 코드로 회사명 조회 (동적 처리)
     */
    private String getCompanyNameByCode(String companyCode) {
        try {
            // 데이터베이스에서 회사 코드로 조회
            return companyRepository.findByCompanyCode(companyCode)
                .map(Company::getCompanyName)
                .orElseGet(() -> {
                    log.warn("등록되지 않은 회사 코드: {}, 코드를 회사명으로 사용", companyCode);
                    return companyCode;  // 매핑이 없으면 코드 그대로 사용
                });
            
        } catch (Exception e) {
            log.error("회사명 조회 실패 - 코드: {}, 코드를 회사명으로 사용", companyCode, e);
            return companyCode;  // 오류 시 코드 그대로 사용
        }
    }

    /**
     * 토픽에서 라인 ID를 추출합니다.
     * 토픽 형식: factory/{companyName}/{lineId}/operations
     * lineId는 숫자로 전달 (예: factory/ms/1/operations -> 1L, factory/ms/2/operations -> 2L)
     */
    private Long extractLineIdFromTopic(String topic) {
        try {
            String[] parts = topic.split("/");
            if (parts.length >= 3) {
                String lineIdStr = parts[2]; // 1, 2, 3 등
                Long lineId = Long.parseLong(lineIdStr);
                log.info("추출된 라인 ID: {}", lineId);
                return lineId;
            }
            log.warn("라인 ID를 추출할 수 없음, 기본값 1 사용: {}", topic);
            return 1L; // 기본값
        } catch (Exception e) {
            log.error("라인 ID 추출 실패, 기본값 1 사용: {}", topic, e);
            return 1L; // 기본값
        }
    }

}