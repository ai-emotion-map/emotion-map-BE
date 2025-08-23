package com.emomap.emomap.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Emotion {

    private final WebClient geminiClient;

    private static final Set<String> KO_ALLOWED = Set.of(
            "가족", "우정", "위로/치유", "외로움", "설렘/사랑", "향수"
    );

    private static final Map<String, String> EN_TO_KO = Map.of(
            "family",     "가족",
            "friendship", "우정",
            "comfort",    "위로/치유",
            "lonely",     "외로움",
            "excitement", "설렘/사랑",
            "nostalgia",  "향수"
    );

    private static final String DEFAULT_TAG = "우정";

    public String classifyIfBlank(String content, String emotionsNullable) {

        if (emotionsNullable != null && !emotionsNullable.isBlank()) {
            String norm = normalizeToKorean(emotionsNullable);
            return norm.isBlank() ? DEFAULT_TAG : norm;
        }

        String prompt = """
                You are an emotion tagger. Pick 1~3 tags that best fit the text.
                Allowed codes: friendship, nostalgia, family, comfort, excitement, lonely.
                Return ONLY comma-separated codes (no spaces, no explanations).
                Text: %s
                """.formatted(content);

        Map<String, Object> req = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = geminiClient.post()
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

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

            String cleaned = normalizeToKorean(out);
            return cleaned.isBlank() ? DEFAULT_TAG : cleaned;

        } catch (Exception e) {
            e.printStackTrace();
            return DEFAULT_TAG;
        }
    }

    private static String normalizeToKorean(String raw) {
        if (raw == null) return "";

        return Arrays.stream(raw.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replaceAll("^[\"'\\[\\](){}]+|[\"'\\[\\](){}]+$", "")) // 따옴표/괄호 제거
                .map(s -> {
                    // 영문 코드면 → 한글로 매핑 (대소문자 허용)
                    String ko = EN_TO_KO.get(s.toLowerCase(Locale.ROOT));
                    if (ko != null) return ko;
                    // 이미 한글이면 그대로(허용 목록만)
                    return KO_ALLOWED.contains(s) ? s : null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .collect(Collectors.joining(","));
    }
}