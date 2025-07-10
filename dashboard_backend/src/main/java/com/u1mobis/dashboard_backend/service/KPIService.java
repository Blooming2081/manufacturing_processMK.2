package com.u1mobis.dashboard_backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.u1mobis.dashboard_backend.entity.KPIData;
import com.u1mobis.dashboard_backend.repository.KPIDataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KPIService {
    
    private final KPIDataRepository kpiDataRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void processKPIData(Map<String, Object> rawData) {
        try {
            // KPI 데이터를 Entity로 변환 및 저장
            KPIData kpiData = convertToEntity(rawData);
            kpiDataRepository.save(kpiData);
            
            log.info("KPI 데이터 저장 완료 - Station: {}, OEE: {}", 
                kpiData.getStationId(), 
                extractOEEValue(rawData));
            
        } catch (Exception e) {
            log.error("KPI 데이터 처리 중 오류 발생", e);
            throw new RuntimeException("KPI 데이터 처리 실패", e);
        }
    }
    
    public Map<String, Object> getLatestKPIData() {
        try {
            List<KPIData> latestKPIs = kpiDataRepository.findLatestKPIByAllStations();
            
            return buildKPIResponse(latestKPIs);
            
        } catch (Exception e) {
            log.error("최신 KPI 데이터 조회 중 오류 발생", e);
            return getDefaultKPIData();
        }
    }
    
    public Map<String, Object> getStationKPI(String stationId) {
        try {
            Optional<KPIData> latestKPI = kpiDataRepository.findTopByStationIdOrderByTimestampDesc(stationId);
            
            if (latestKPI.isPresent()) {
                return convertKPIToMap(latestKPI.get());
            } else {
                return Map.of(
                    "stationId", stationId,
                    "message", "해당 스테이션의 KPI 데이터가 없습니다."
                );
            }
            
        } catch (Exception e) {
            log.error("스테이션 KPI 조회 중 오류 발생", e);
            return Map.of("error", "스테이션 KPI 조회 실패");
        }
    }
    
    public Map<String, Object> getFactoryKPISummary() {
        try {
            List<KPIData> allStationKPIs = kpiDataRepository.findLatestKPIByAllStations();
            
            if (allStationKPIs.isEmpty()) {
                return getDefaultFactorySummary();
            }
            
            // 공장 전체 KPI 계산
            double avgOEE = allStationKPIs.stream()
                .mapToDouble(kpi -> extractDoubleFromJson(kpi.getOeeData(), "value"))
                .average()
                .orElse(0.0);
                
            double avgFTY = allStationKPIs.stream()
                .mapToDouble(kpi -> extractDoubleFromJson(kpi.getFtyData(), "value"))
                .average()
                .orElse(0.0);
                
            double avgOTD = allStationKPIs.stream()
                .mapToDouble(kpi -> extractDoubleFromJson(kpi.getOtdData(), "value"))
                .average()
                .orElse(0.0);
            
            double totalThroughput = allStationKPIs.stream()
                .mapToDouble(kpi -> extractDoubleFromJson(kpi.getThroughputData(), "value"))
                .sum();
            
            // 평균 사이클 타임 계산 (90초 기준)
            double avgCycleTime = allStationKPIs.stream()
                .mapToDouble(kpi -> extractDoubleFromJson(kpi.getCycleTimeData(), "value"))
                .average()
                .orElse(90.0);
            
            // 시간당 생산량 계산 (3600초 / 평균 사이클 타임)
            double hourlyRate = avgCycleTime > 0 ? 3600.0 / avgCycleTime : 0.0;
            
            // 현재 생산량 (총 처리량 기준)
            int currentProduction = (int) Math.round(totalThroughput);
            
            // 일일 목표 (20대/시간 * 24시간 = 480대)
            int dailyTarget = 480;
            
            // 전체 품질 점수 계산
            double overallQualityScore = allStationKPIs.stream()
                .mapToDouble(kpi -> extractDoubleFromJson(kpi.getQualityData(), "value"))
                .average()
                .orElse(0.95); // 기본값 95%
            
            // Dashboard.jsx가 기대하는 구조로 응답 생성
            return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "production", Map.of(
                    "current", currentProduction,
                    "target", dailyTarget,
                    "hourlyRate", Math.round(hourlyRate * 10.0) / 10.0,
                    "cycleTime", Math.round(avgCycleTime * 10.0) / 10.0
                ),
                "kpi", Map.of(
                    "oee", Math.round(avgOEE * 100.0) / 100.0,
                    "fty", Math.round(avgFTY * 100.0) / 100.0,
                    "otd", Math.round(avgOTD * 100.0) / 100.0
                ),
                "quality", Map.of(
                    "overallScore", Math.round(overallQualityScore * 10000.0) / 10000.0
                ),
                "active_stations", allStationKPIs.size(),
                "last_updated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            );
            
        } catch (Exception e) {
            log.error("공장 KPI 요약 조회 중 오류 발생", e);
            return getDefaultFactorySummary();
        }
    }
    
    private KPIData convertToEntity(Map<String, Object> rawData) {
        KPIData entity = KPIData.builder()
            .stationId((String) rawData.get("station_id"))
            .timestamp(parseTimestamp((String) rawData.get("timestamp")))
            .totalCycles((Integer) rawData.getOrDefault("total_cycles", 0))
            .runtimeHours(((Number) rawData.getOrDefault("runtime_hours", 0.0)).doubleValue())
            .build();
        
        // JSON 데이터 저장
        if (rawData.containsKey("oee")) {
            entity.setOeeDataFromObject(rawData.get("oee"));
        }
        if (rawData.containsKey("fty")) {
            entity.setFtyDataFromObject(rawData.get("fty"));
        }
        if (rawData.containsKey("otd")) {
            entity.setOtdDataFromObject(rawData.get("otd"));
        }
        if (rawData.containsKey("quality_score")) {
            entity.setQualityDataFromObject(rawData.get("quality_score"));
        }
        if (rawData.containsKey("throughput")) {
            entity.setThroughputDataFromObject(rawData.get("throughput"));
        }
        if (rawData.containsKey("avg_cycle_time")) {
            entity.setCycleTimeDataFromObject(rawData.get("avg_cycle_time"));
        }
        
        return entity;
    }
    
    private Map<String, Object> buildKPIResponse(List<KPIData> kpiList) {
        if (kpiList.isEmpty()) {
            return getDefaultKPIData();
        }
        
        // Dashboard.jsx가 기대하는 배열 형식으로 스테이션 데이터 생성
        List<Map<String, Object>> stationArray = new ArrayList<>();
        for (KPIData kpi : kpiList) {
            Map<String, Object> stationData = convertKPIToSimpleMap(kpi);
            stationArray.add(stationData);
        }
        
        // 전체 요약
        double avgOEE = kpiList.stream()
            .mapToDouble(kpi -> extractDoubleFromJson(kpi.getOeeData(), "value"))
            .average()
            .orElse(0.0);
            
        double avgFTY = kpiList.stream()
            .mapToDouble(kpi -> extractDoubleFromJson(kpi.getFtyData(), "value"))
            .average()
            .orElse(0.0);
        
        // Dashboard.jsx는 stations 배열을 직접 사용하므로 배열을 바로 반환
        return Map.of(
            "stations", stationArray,
            "summary", Map.of(
                "avg_oee", Math.round(avgOEE * 100.0) / 100.0,
                "avg_fty", Math.round(avgFTY * 100.0) / 100.0,
                "total_stations", kpiList.size(),
                "timestamp", LocalDateTime.now().toString()
            )
        );
    }
    
    private Map<String, Object> convertKPIToMap(KPIData kpiData) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("stationId", kpiData.getStationId());
        result.put("timestamp", kpiData.getTimestamp().toString());
        result.put("totalCycles", kpiData.getTotalCycles());
        result.put("runtimeHours", kpiData.getRuntimeHours());
        
        // JSON 데이터를 Map으로 변환
        try {
            if (kpiData.getOeeData() != null) {
                result.put("oee", objectMapper.convertValue(kpiData.getOeeData(), Map.class));
            }
            if (kpiData.getFtyData() != null) {
                result.put("fty", objectMapper.convertValue(kpiData.getFtyData(), Map.class));
            }
            if (kpiData.getOtdData() != null) {
                result.put("otd", objectMapper.convertValue(kpiData.getOtdData(), Map.class));
            }
            if (kpiData.getQualityData() != null) {
                result.put("quality_score", objectMapper.convertValue(kpiData.getQualityData(), Map.class));
            }
            if (kpiData.getThroughputData() != null) {
                result.put("throughput", objectMapper.convertValue(kpiData.getThroughputData(), Map.class));
            }
        } catch (Exception e) {
            log.warn("JSON 데이터 변환 중 오류 발생", e);
        }
        
        return result;
    }
    
    private Map<String, Object> convertKPIToSimpleMap(KPIData kpiData) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("stationId", kpiData.getStationId());
        result.put("timestamp", kpiData.getTimestamp().toString());
        result.put("totalCycles", kpiData.getTotalCycles());
        result.put("runtimeHours", kpiData.getRuntimeHours());
        
        // Dashboard.jsx가 기대하는 간단한 숫자 형식으로 KPI 값 추출
        result.put("oee", extractDoubleFromJson(kpiData.getOeeData(), "value"));
        result.put("fty", extractDoubleFromJson(kpiData.getFtyData(), "value"));
        result.put("otd", extractDoubleFromJson(kpiData.getOtdData(), "value"));
        result.put("qualityScore", extractDoubleFromJson(kpiData.getQualityData(), "value"));
        result.put("throughput", extractDoubleFromJson(kpiData.getThroughputData(), "value"));
        result.put("cycleTime", extractDoubleFromJson(kpiData.getCycleTimeData(), "value"));
        
        return result;
    }
    
    private double extractDoubleFromJson(com.fasterxml.jackson.databind.JsonNode jsonNode, String key) {
        if (jsonNode != null && jsonNode.has(key)) {
            return jsonNode.get(key).asDouble();
        }
        return 0.0;
    }
    
    private double extractOEEValue(Map<String, Object> rawData) {
        if (rawData.containsKey("oee")) {
            Object oeeData = rawData.get("oee");
            if (oeeData instanceof Number) {
                return ((Number) oeeData).doubleValue();
            } else if (oeeData instanceof Map) {
                Map<String, Object> oeeMap = (Map<String, Object>) oeeData;
                if (oeeMap.containsKey("value")) {
                    return ((Number) oeeMap.get("value")).doubleValue();
                }
            }
        }
        return 0.0;
    }
    
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return LocalDateTime.parse(timestamp.replace("Z", ""));
        } catch (Exception e) {
            log.warn("타임스탬프 파싱 실패: {}", timestamp);
            return LocalDateTime.now();
        }
    }
    
    private Map<String, Object> getDefaultKPIData() {
        return Map.of(
            "stations", new ArrayList<>(), // 빈 배열로 수정
            "summary", Map.of(
                "avg_oee", 0.0,
                "avg_fty", 0.0,
                "total_stations", 0,
                "timestamp", LocalDateTime.now().toString()
            ),
            "message", "KPI 데이터가 없습니다 (시뮬렬이션 모드)"
        );
    }
    
    private Map<String, Object> getDefaultFactorySummary() {
        return Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "production", Map.of(
                "current", 0,
                "target", 480,
                "hourlyRate", 0.0,
                "cycleTime", 90.0
            ),
            "kpi", Map.of(
                "oee", 0.0,
                "fty", 0.0,
                "otd", 0.0
            ),
            "quality", Map.of(
                "overallScore", 0.0
            ),
            "active_stations", 0,
            "last_updated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            "message", "공장 KPI 데이터가 없습니다"
        );
    }
}