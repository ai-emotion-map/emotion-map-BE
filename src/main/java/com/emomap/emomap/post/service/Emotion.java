package com.emomap.emomap.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Emotion {

    // gemini api 호출용 webclient
    private final WebClient geminiClient;

    // 감정 태그 허용 목록
    private static final List<String> ALLOWED = List.of(
            "friendship","nostalgia","family","comfort","excitement","lonely"
    );

    // 감정 태그가 비어 있으면 ai로 분류
    public String classifyIfBlank(String content, String emotionsNullable) {

        // 사용자가 감정 태그를 직접 넣은 경우에는 그대로 쓰게 함
        if (emotionsNullable != null && !emotionsNullable.isBlank()) {
            return emotionsNullable.trim();
        }

        // gemini에 보낼 프롬프트
        String prompt = """
                You are an emotion tagger. Pick 1~3 tags that best fit the text.
                Allowed: friendship, nostalgia, family, comfort, excitement, lonely.
                Return ONLY comma-separated codes, no spaces, no explanations.
                Text: %s
                """.formatted(content);

        // gemini api 요청 json
        Map<String, Object> req = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            // gemini api 호출
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = geminiClient.post()
                    .bodyValue(req)              // 요청 바디
                    .retrieve()                  // 응답 가져오기
                    .bodyToMono(Map.class)       // Map 형태로 변환시킴
                    .block();                    // 동기 방식으로 기다림

            // 응답에서 감정 태그만 뽑는 것임
            String out = "";
            if (resp != null) {
                var candidates = (List<Map<String, Object>>) resp.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    var contentMap = (Map<String, Object>) candidates.get(0).get("content");
                    if (contentMap != null) {
                        var parts = (List<Map<String, Object>>) contentMap.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Object textObj = parts.get(0).get("text");
                            if (textObj != null) out = textObj.toString();
                        }
                    }
                }
            }

            // 태그 정리 후 리턴하는데 아무것도 없으면 friendship을 반환해줌
            String cleaned = normalize(out);
            return cleaned.isBlank() ? "friendship" : cleaned;

        } catch (Exception e) {
            e.printStackTrace(); // 에러를 찍는 것임
            return "friendship"; // 만약 실패했으면 friendship이 기본 default 값으로 반환됨
        }
    }

    // ai가 준 문자열을 허용 목록에 맞게 정리해놓은 것임
    private static String normalize(String raw) {
        if (raw == null) return "";
        return java.util.Arrays.stream(raw.trim().split("[,\\s]+"))         // 콤마나 공백으로 분리함
                .map(String::trim)                                               // 공백을 제거함
                .filter(ALLOWED::contains)                                       // 허용된 태그만 걸러지게 함
                .distinct()                                                      // 중복을 제거함
                .limit(3)                                                // 최대 3개까지만 나오게 함
                .reduce((a, b) -> a + "," + b)                      // 합치는 것임
                .orElse("");                                              // 없으면 빈 문자열이 나옴
    }
}
