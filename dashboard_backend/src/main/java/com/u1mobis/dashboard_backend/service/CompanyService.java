package com.u1mobis.dashboard_backend.service;

import com.u1mobis.dashboard_backend.dto.CompanyRegistrationDTO;
import com.u1mobis.dashboard_backend.dto.CompanyResponseDTO;
import com.u1mobis.dashboard_backend.entity.Company;
import com.u1mobis.dashboard_backend.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;

    /**
     * 회사 등록
     */
    public CompanyResponseDTO registerCompany(CompanyRegistrationDTO registrationDTO) {
        try {
            // 회사명 중복 확인
            if (companyRepository.existsByCompanyName(registrationDTO.getCompanyName())) {
                return CompanyResponseDTO.builder()
                        .success(false)
                        .message("이미 등록된 회사명입니다.")
                        .build();
            }

            // 회사코드 자동 생성 (DTO에서 코드가 없는 경우)
            String companyCode = registrationDTO.getCompanyCode();
            if (companyCode == null || companyCode.trim().isEmpty()) {
                companyCode = generateUniqueCompanyCode();
            } else {
                companyCode = companyCode.toUpperCase();
                // 회사코드 중복 확인
                if (companyRepository.existsByCompanyCode(companyCode)) {
                    return CompanyResponseDTO.builder()
                            .success(false)
                            .message("이미 사용중인 회사코드입니다.")
                            .build();
                }
            }

            // 회사 생성 및 저장
            Company company = Company.builder()
                    .companyName(registrationDTO.getCompanyName())
                    .companyCode(companyCode)
                    .build();

            Company savedCompany = companyRepository.save(company);

            log.info("회사 등록 성공: {} (코드: {})", savedCompany.getCompanyName(), savedCompany.getCompanyCode());

            return CompanyResponseDTO.builder()
                    .success(true)
                    .message("회사가 성공적으로 등록되었습니다.")
                    .companyId(savedCompany.getCompanyId())
                    .companyName(savedCompany.getCompanyName())
                    .companyCode(savedCompany.getCompanyCode())
                    .createdAt(savedCompany.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("회사 등록 중 오류 발생", e);
            return CompanyResponseDTO.builder()
                    .success(false)
                    .message("회사 등록 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 회사코드로 회사 조회
     */
    @Transactional(readOnly = true)
    public Optional<Company> findByCompanyCode(String companyCode) {
        return companyRepository.findByCompanyCode(companyCode.toUpperCase());
    }

    /**
     * 회사코드 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsByCompanyCode(String companyCode) {
        return companyRepository.existsByCompanyCode(companyCode.toUpperCase());
    }

    /**
     * 회사 정보 조회
     */
    @Transactional(readOnly = true)
    public CompanyResponseDTO getCompanyInfo(String companyCode) {
        Optional<Company> company = findByCompanyCode(companyCode);
        
        if (company.isPresent()) {
            Company comp = company.get();
            return CompanyResponseDTO.builder()
                    .success(true)
                    .companyId(comp.getCompanyId())
                    .companyName(comp.getCompanyName())
                    .companyCode(comp.getCompanyCode())
                    .createdAt(comp.getCreatedAt())
                    .build();
        } else {
            return CompanyResponseDTO.builder()
                    .success(false)
                    .message("해당 회사코드를 찾을 수 없습니다.")
                    .build();
        }
    }

    /**
     * 회사명 중복 체크
     */
    @Transactional(readOnly = true)
    public boolean existsByCompanyName(String companyName) {
        return companyRepository.existsByCompanyName(companyName);
    }

    /**
     * 모든 회사 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CompanyResponseDTO> getAllCompanies() {
        List<Company> companies = companyRepository.findAll();
        
        return companies.stream()
                .map(company -> CompanyResponseDTO.builder()
                        .success(true)
                        .companyId(company.getCompanyId())
                        .companyName(company.getCompanyName())
                        .companyCode(company.getCompanyCode())
                        .createdAt(company.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 유니크한 회사코드 생성
     */
    private String generateUniqueCompanyCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String companyCode;
        
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            companyCode = sb.toString();
        } while (companyRepository.existsByCompanyCode(companyCode));
        
        return companyCode;
    }
}