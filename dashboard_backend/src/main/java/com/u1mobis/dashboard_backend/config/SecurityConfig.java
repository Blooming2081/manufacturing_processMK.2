package com.u1mobis.dashboard_backend.config;

import com.u1mobis.dashboard_backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize, @PostAuthorize 사용 가능
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
            .csrf(csrf -> csrf.disable()) // JWT 사용시 CSRF 비활성화
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안함
            .authorizeHttpRequests(authz -> authz
                // 시뮬레이터 및 Unity API 우선 허용 (가장 먼저 매칭)
                .requestMatchers("/api/simulator/**").permitAll()
                .requestMatchers("/api/unity/**").permitAll()
                .requestMatchers("/api/digital-twin/**").permitAll()
                // 인증 없이 접근 가능한 경로들
                .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                .requestMatchers("/api/user/refresh-token").permitAll()
                .requestMatchers("/api/company/**").permitAll() // 모든 company 경로 허용
                // 웹소켓 관련 경로 허용
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/test/**").permitAll() // 테스트 API 허용
                // 테스트용 API 엔드포인트 허용
                .requestMatchers("/api/dashboard", "/api/production/status", "/api/kpi/realtime").permitAll()
                .requestMatchers("/api/environment/**", "/api/stock", "/api/stocks/**").permitAll()
                .requestMatchers("/api/click/**", "/api/iot/**", "/api/conveyor/**").permitAll()
                // 특정 회사 경로 허용 (u1, DEFAULT 등) - 먼저 명시
                .requestMatchers("/api/u1/**").permitAll()
                .requestMatchers("/api/DEFAULT/**").permitAll()
                // 회사별 API 경로 허용 (패턴 매칭) - 더 구체적으로
                .requestMatchers("/api/*/dashboard").permitAll()
                .requestMatchers("/api/*/kpi/**").permitAll()
                .requestMatchers("/api/*/environment/**").permitAll()
                .requestMatchers("/api/*/production/**").permitAll()
                .requestMatchers("/api/*/conveyor/**").permitAll()
                .requestMatchers("/api/*/stock/**").permitAll()
                .requestMatchers("/api/*/click/**").permitAll()
                // 모든 회사별 API 경로 허용 (가장 마지막에)
                .requestMatchers("/api/*/**").permitAll()
                // 정적 리소스 허용 (필요시)
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // 인증이 필요한 정적 경로들
                .requestMatchers("/api/user/me", "/api/user/logout").authenticated()
                // 회사별 사용자 정보는 인증 필요
                .requestMatchers("/api/user/{companyName}").authenticated()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider()) // 커스텀 인증 프로바이더 설정
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 추가
            
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 기존의 setAllowedOrigins 대신 setAllowedOriginPatterns 사용
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*", 
            "https://localhost:*",
            "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // preflight 캐시 시간
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}