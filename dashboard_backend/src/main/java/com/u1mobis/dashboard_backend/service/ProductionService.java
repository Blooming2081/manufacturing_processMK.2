package com.u1mobis.dashboard_backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.u1mobis.dashboard_backend.entity.CurrentProduction;
import com.u1mobis.dashboard_backend.entity.ProductionCompleted;
import com.u1mobis.dashboard_backend.entity.QualityRecord;
import com.u1mobis.dashboard_backend.repository.CurrentProductionRepository;
import com.u1mobis.dashboard_backend.repository.ProductionCompletedRepository;
import com.u1mobis.dashboard_backend.repository.QualityRecordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductionService {
    
    private final CurrentProductionRepository currentProductionRepository;
    private final ProductionCompletedRepository productionCompletedRepository;
    private final QualityRecordRepository qualityRecordRepository;
    
    // 새 제품 생산 시작
    public CurrentProduction startProduction(String productId, LocalDateTime dueDate) {
        CurrentProduction production = CurrentProduction.builder()
            .productId(productId)
            .startTime(LocalDateTime.now())
            .dueDate(dueDate)
            .reworkCount(0)
            .currentStation("STATION_01")
            .status("PROCESSING")
            .build();
            
        return currentProductionRepository.save(production);
    }
    
    // 품질 검사 결과 처리 (MQTT에서 호출)
    public void processQualityCheck(String productId, String robotId, String result, String defectReason, Double torqueValue) {
        // 1. 품질 기록 저장
        QualityRecord qualityRecord = QualityRecord.builder()
            .productId(productId)
            .robotId(robotId)
            .timestamp(LocalDateTime.now())
            .result(result)
            .defectReason(defectReason)
            .torqueValue(torqueValue)
            .build();
            
        qualityRecordRepository.save(qualityRecord);
        
        // 2. 불량 시 재작업 처리
        if ("FAIL".equals(result)) {
            currentProductionRepository.incrementReworkCount(productId);
            log.info("재작업 카운트 증가: 제품 ID = {}, 로봇 ID = {}, 불량사유 = {}", 
                    productId, robotId, defectReason);
        }
    }
    
    // 생산 시작 처리 (MQTT에서 호출)
    public CurrentProduction startProduction(String companyName, Long lineId, String productId, Integer targetQuantity, LocalDateTime dueDate) {
        log.info("생산 시작 처리 - 회사: {}, 라인: {}, 제품: {}", companyName, lineId, productId);
        
        CurrentProduction production = CurrentProduction.builder()
            .productId(productId)
            .lineId(lineId)
            .startTime(LocalDateTime.now())
            .dueDate(dueDate)
            .status("PROCESSING")
            .currentStation("START")
            .reworkCount(0)
            .build();
            
        CurrentProduction saved = currentProductionRepository.save(production);
        log.info("생산 시작 저장 완료 - 제품: {}", productId);
        return saved;
    }
    
    // 생산 완료 처리 (MQTT에서 호출)
    public ProductionCompleted completeProduction(String companyName, Long lineId, String productId, Double cycleTime, String quality, LocalDateTime dueDate) {
        // 1. 현재 생산 정보 조회
        Optional<CurrentProduction> currentOpt = currentProductionRepository.findById(productId);
        if (currentOpt.isEmpty()) {
            throw new RuntimeException("생산 중인 제품을 찾을 수 없습니다: " + productId);
        }
        
        CurrentProduction current = currentOpt.get();
        boolean isFirstTimePass = current.getReworkCount() == 0;
        boolean isOnTime = LocalDateTime.now().isBefore(dueDate) || LocalDateTime.now().isEqual(dueDate);
        
        // 2. 완료 기록 저장
        ProductionCompleted completed = ProductionCompleted.builder()
            .productId(productId)
            .timestamp(LocalDateTime.now())
            .cycleTime(cycleTime)
            .quality(quality)
            .dueDate(dueDate)
            .isOnTime(isOnTime)
            .isFirstTimePass(isFirstTimePass)
            .build();
            
        productionCompletedRepository.save(completed);
        
        // 3. 현재 생산 상태 업데이트
        current.setStatus("COMPLETED");
        currentProductionRepository.save(current);
        
        log.info("생산 완료: 제품 ID = {}, 품질 = {}, 정시납기 = {}, 일발통과 = {}", 
                productId, quality, isOnTime, isFirstTimePass);
                
        return completed;
    }
    
    // 현재 생산 현황 조회
    public Map<String, Object> getCurrentProductionStatus() {
        List<CurrentProduction> processing = currentProductionRepository.findByStatus("PROCESSING");
        Long todayCompleted = productionCompletedRepository.countTodayCompletedProducts();
        Long todayGood = productionCompletedRepository.countTodayGoodProducts();
        
        return Map.of(
            "processing_count", processing.size(),
            "today_completed", todayCompleted,
            "today_good", todayGood,
            "processing_products", processing
        );
    }
}
