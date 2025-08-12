package com.emomap.emomap.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .httpBasic(basic -> basic.disable()) // 기본 인증 팝업 끔
                .formLogin(form -> form.disable()) // 폼 로그인도 끔
                .authorizeHttpRequests(auth -> auth
                        // Swagger 경로 허용
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // API 전부 허용
                        .requestMatchers(
                                "/users/**",
                                "/posts/**"
                        ).permitAll()
                        // 나머지 요청
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
