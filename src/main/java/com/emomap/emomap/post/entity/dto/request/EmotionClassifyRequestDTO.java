package com.emomap.emomap.post.entity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmotionClassifyRequestDTO(
        @Schema(description = "분석할 본문", example = "친구와 학교 근처에서 저녁 먹었다. 너무 반가웠다.")
        String content
) {}
