package com.u1mobis.dashboard_backend.controller;

import com.u1mobis.dashboard_backend.dto.CompanyRegistrationDTO;
import com.u1mobis.dashboard_backend.dto.CompanyResponseDTO;
import com.u1mobis.dashboard_backend.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 테스트용 GET 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        log.info("=== Company Controller 테스트 엔드포인트 호출 ===");
        return ResponseEntity.ok("Company Controller가 정상 작동합니다!");
    }

    /**
     * 회사 등록
     */
    @PostMapping("/register")
    public ResponseEntity<CompanyResponseDTO> registerCompany(@RequestBody CompanyRegistrationDTO registrationDTO) {
        log.info("=== 회사 등록 요청 수신 ===");
        log.info("회사명: {}", registrationDTO.getCompanyName());
        log.info("회사코드: {}", registrationDTO.getCompanyCode());
        log.info("요청 전체: {}", registrationDTO);
        
        try {
            CompanyResponseDTO response = companyService.registerCompany(registrationDTO);
            log.info("회사 등록 처리 결과: success={}, message={}", response.isSuccess(), response.getMessage());
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("회사 등록 중 예외 발생", e);
            CompanyResponseDTO errorResponse = CompanyResponseDTO.builder()
                    .success(false)
                    .message("서버 내부 오류가 발생했습니다: " + e.getMessage())
                    .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 회사코드로 회사 정보 조회
     */
    @GetMapping("/code/{companyCode}")
    public ResponseEntity<CompanyResponseDTO> getCompanyByCode(@PathVariable String companyCode) {
        log.info("회사 정보 조회 요청: {}", companyCode);
        
        CompanyResponseDTO response = companyService.getCompanyInfo(companyCode);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 회사명 중복 체크
     */
    @GetMapping("/check-name/{companyName}")
    public ResponseEntity<Boolean> checkCompanyNameExists(@PathVariable String companyName) {
        log.info("회사명 중복 확인: {}", companyName);
        
        boolean exists = companyService.existsByCompanyName(companyName);
        return ResponseEntity.ok(exists);
    }

    /**
     * 회사코드 존재 여부 확인
     */
    @GetMapping("/exists/{companyCode}")
    public ResponseEntity<Boolean> checkCompanyCodeExists(@PathVariable String companyCode) {
        log.info("회사코드 존재 확인: {}", companyCode);
        
        boolean exists = companyService.existsByCompanyCode(companyCode);
        return ResponseEntity.ok(exists);
    }

    /**
     * 모든 회사 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<List<CompanyResponseDTO>> getAllCompanies() {
        log.info("모든 회사 목록 조회 요청");
        
        List<CompanyResponseDTO> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }
}