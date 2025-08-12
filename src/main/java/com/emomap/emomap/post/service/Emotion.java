package com.emomap.emomap.post.service;   // 포스트 관련 서비스 패키지

import com.fasterxml.jackson.databind.JsonNode;   // JSON 파싱용 Jackson
import lombok.RequiredArgsConstructor;           // final 필드 생성자 자동 주입 lombok
import org.springframework.beans.factory.annotation.Value; // yml에서 값 받기
import org.springframework.stereotype.Service;   // 서비스 빈 등록
import org.springframework.web.reactive.function.client.WebClient; // HTTP 호출 클라이언트

import java.util.*;                               // List, Map 등
import java.util.stream.Collectors;               // 스트림을 문자열 변환 해줌

@Service                                         // 이 클래스가 서비스 레이어임
@RequiredArgsConstructor                         // final 필드 자동 생성자
public class Emotion {

    private final WebClient openAiClient;        // OpenAI API 호출용 WebClient

    @Value("${app.openai.model}")
    private String model;                        // 사용할 GPT 모델명

    // 사용할 수 있는 감정 코드들 미리 고정
    private static final List<String> ALLOWED = List.of(
            "friendship","nostalgia","family","comfort","excitement","lonely"
    );

    /* emotions가 비어있을 때 GPT 호출해서 "code1,code2" 형태로 반환 */
    public String classifyIfBlank(String content, String emotionsNullable) {
        // 이미 emotions가 들어있으면 GPT 안 불러도 되니까 그대로 반환함
        if (emotionsNullable != null && !emotionsNullable.isBlank()) return emotionsNullable;

        /**
        // GPT한테 역할 설명: 1~3개 감정 코드만 뽑아달라는 말
        String sys = """
            You are an emotion tagger. Pick 1~3 tags that best fit the text.
            Allowed codes: friendship, nostalgia, family, comfort, excitement, lonely.
            Return ONLY comma-separated codes without spaces or explanations.
            """;
        // 실제 본문 내용 전달해줌
        String user = "text: " + content;

        // OpenAI API에 보낼 요청 바디 구성
        Map<String, Object> req = Map.of(
                "model", model, // 사용할 모델명
                "messages", List.of(
                        Map.of("role", "system", "content", sys), // 역할 정의
                        Map.of("role", "user", "content", user)   // 실제 본문
                ),
                "temperature", 0 // 랜덤 없이 딱 정해진 태그 뽑게 함
        );

        // WebClient로 OpenAI API 호출
        String raw = openAiClient.post()             // POST 요청
                .uri("/chat/completions")            // 채팅 완성 API 엔드포인트
                .bodyValue(req)                      // 요청 바디 세팅
                .retrieve()                          // 응답 가져오기
                .bodyToMono(JsonNode.class)          // JSON으로 변환
                .map(n -> n.at("/choices/0/message/content").asText("")) // 응답에서 결과 텍스트만 추출
                .block();                            // 동기식으로 결과를 받을 때까지 대기

        // 응답이 null이면 기본값 default 반환
        if (raw == null) return "default";

        // GPT 응답에서 허용된 코드를 최대 3개까지 반환
        String out = Arrays.stream(raw.trim().split("[,\\s]+")) // 쉼표 및 공백 기준으로 쪼갬
                .map(String::trim)                              // 양쪽 공백 제거
                .filter(ALLOWED::contains)                      // 허용된 코드만
                .distinct()                                     // 중복 제거
                .limit(3)                                       // 최대 3개
                .collect(Collectors.joining(","));              // 다시 쉼표로 합침

        // 최종 결과가 비었으면 default, 아니면 그대로 반환
        return out.isBlank() ? "default" : out;
         **/

        return "friendship"; // 테스트 시 GPT 호출 대신 더미 값 리턴
    }
}
