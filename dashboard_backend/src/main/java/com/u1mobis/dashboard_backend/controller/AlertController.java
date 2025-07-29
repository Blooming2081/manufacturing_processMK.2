package com.u1mobis.dashboard_backend.controller;

import com.u1mobis.dashboard_backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;

    @MessageMapping("/subscribe")
    public void subscribeToAlerts(@Payload Map<String, String> payload, 
                                SimpMessageHeaderAccessor headerAccessor) {
        String companyName = payload.get("companyName");
        String userId = payload.get("userId");
        
        // 세션에 회사 이름 저장
        headerAccessor.getSessionAttributes().put("companyName", companyName);
        headerAccessor.getSessionAttributes().put("userId", userId);
        
        log.info("사용자 {}가 회사 {} 알림 구독", userId, companyName);
    }
}