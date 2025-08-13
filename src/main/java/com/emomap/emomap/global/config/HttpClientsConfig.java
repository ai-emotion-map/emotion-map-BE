package com.emomap.emomap.global.config;               // 전역 설정 패키지

import org.springframework.beans.factory.annotation.Value;  // 키랑 모델 읽을 때 @Value로 가져옴
import org.springframework.context.annotation.Bean;       // @Bean 등록
import org.springframework.context.annotation.Configuration; // 설정 클래스 표시
import org.springframework.http.*;                        // 헤더 및 미디어 타입
import org.springframework.web.reactive.function.client.WebClient; // HTTP 클라이언트

@Configuration
public class HttpClientsConfig {

    @Bean
    public WebClient geminiClient(@Value("${app.gemini.api-key}") String apiKey) {
        // Gemini API 호출 시 key를 queryParam으로 붙이기 위해서 baseUrl에는 key까지 포함함
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // JSON 요청
                .build();
    }

    @Bean                                               // kakaoLocalClient
    public WebClient kakaoLocalClient(@Value("${app.kakao.rest-key}") String key) { // 카카오 REST key
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")      // 카카오 로컬 API 기본 URL
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + key) // 인증 헤더
                .build();
    }
}
