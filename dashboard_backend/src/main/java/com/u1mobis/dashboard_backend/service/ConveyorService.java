package com.u1mobis.dashboard_backend.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.u1mobis.dashboard_backend.entity.ConveyorStatus;
import com.u1mobis.dashboard_backend.repository.ConveyorStatusRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConveyorService {

    private final ConveyorStatusRepository conveyorStatusRepository;

    // 컨베이어 상태 저장 (MQTT에서 호출)
    public ConveyorStatus saveConveyorStatus(String companyName, Long lineId, String command, String reason) {
        log.info("컨베이어 상태 저장 시작 - 회사: {}, 라인: {}, 명령: {}, 이유: {}", companyName, lineId, command, reason);
        
        ConveyorStatus status = ConveyorStatus.builder()
                .timestamp(LocalDateTime.now())
                .lineId(lineId)
                .command(command)
                .reason(reason)
                .build();

        ConveyorStatus saved = conveyorStatusRepository.save(status);
        log.info("컨베이어 상태 저장 완료");
        return saved;
    }

    // 최신 컨베이어 상태 조회
    public Map<String, Object> getCurrentConveyorStatus() {
        Optional<ConveyorStatus> latest = conveyorStatusRepository.findTopByOrderByTimestampDesc();

        if (latest.isPresent()) {
            ConveyorStatus status = latest.get();
            return Map.of(
                    "command", status.getCommand(),
                    "reason", status.getReason(),
                    "timestamp", status.getTimestamp().toString(), // 문자열로 변환
                    "status", getConveyorDisplayStatus(status.getCommand()));
        }

        return Map.of(
                "command", "UNKNOWN",
                "reason", "NO_DATA",
                "status", "OFFLINE");
    }

    private String getConveyorDisplayStatus(String command) {
        switch (command) {
            case "START":
                return "운영중";
            case "STOP":
                return "정지";
            case "PAUSE":
                return "일시정지";
            case "EMERGENCY_STOP":
                return "비상정지";
            default:
                return "알 수 없음";
        }
    }
}
