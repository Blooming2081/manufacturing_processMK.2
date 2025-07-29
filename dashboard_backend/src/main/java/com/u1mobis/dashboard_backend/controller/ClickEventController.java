package com.u1mobis.dashboard_backend.controller;

import com.u1mobis.dashboard_backend.service.ClickEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@RestController
@RequestMapping("/api/{companyName}/click")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*"})
@Slf4j
public class ClickEventController {

    @Autowired
    private ClickEventService clickEventService;

    // CORS preflight 요청 처리
    @RequestMapping(value = "/object", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type")
                .build();
    }

    // Unity에서 클릭 이벤트 처리
    @PostMapping("/object")
    @CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*"}, methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<?> handleObjectClick(@RequestBody Map<String, Object> clickData) {
        try {
            String objectType = (String) clickData.get("objectType");
            String objectId = (String) clickData.get("objectId");
            
            Map<String, Object> response = clickEventService.handleClick(objectType, objectId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // 로봇 상태 조회
    @GetMapping("/robot/{robotId}")
    public ResponseEntity<?> getRobotStatus(@PathVariable String robotId) {
        try {
            Map<String, Object> robotStatus = clickEventService.getRobotStatusData(robotId);
            return ResponseEntity.ok(robotStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Robot not found: " + robotId);
        }
    }


    // 공정 정보 조회
    @GetMapping("/station/{stationId}")
    public ResponseEntity<?> getStationInfo(@PathVariable String stationId) {
        try {
            Map<String, Object> station = clickEventService.getStationData(stationId);
            return ResponseEntity.ok(station);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Station not found: " + stationId);
        }
    }

    // 제품 정보 조회
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductInfo(@PathVariable String productId) {
        try {
            Map<String, Object> product = clickEventService.getProductData(productId);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Product not found: " + productId);
        }
    }
}