package com.emomap.emomap.post.entity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record EmotionClassifyResponseDTO(
        @Schema(description = "필터링된 감정 태그(최대 3개)")
        List<String> tags,
        @Schema(description = "원본 모델 응답")
        String raw
) {}
