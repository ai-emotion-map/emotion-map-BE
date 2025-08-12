package com.emomap.emomap.global.config;               // 전역 설정 패키지


import org.springframework.beans.factory.annotation.Value;  // 키랑 모델 읽을 때 @Value로 가져옴
import org.springframework.context.annotation.Bean;       // @Bean 등록
import org.springframework.context.annotation.Configuration; // 설정 클래스 표시
import org.springframework.http.*;                        // 헤더 및 미디어 타입
import org.springframework.web.reactive.function.client.WebClient; // HTTP 클라이언트

@Configuration
public class HttpClientsConfig {

    @Bean                                               // openAiClient
    public WebClient openAiClient(@Value("${app.openai.api-key}") String key) { // 환경변수에서 키 가져옴
        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")   // OpenAI API 기본 URL
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key) // 인증 헤더
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
