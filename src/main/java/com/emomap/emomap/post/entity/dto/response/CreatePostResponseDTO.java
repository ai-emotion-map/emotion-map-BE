package com.emomap.emomap.post.entity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CreatePostResponseDTO(
        @Schema(example = "6") Long id,
        @Schema(example = "37.6") double lat,
        @Schema(example = "127.03") double lng,
        @Schema(example = "서울특별시 성북구 종암로23길 35") String roadAddress,

        // 리스트 전체의 예시를 문자열로
        @Schema(
                description = "감정 태그(한글만 사용)",
                example = "[\"우정\",\"향수\"]"
        )
        List<String> tags,

        @Schema(
                description = "업로드 이미지 URL",
                example = "[\"/uploads/2025-08-22/xxxx_cat.jpg\"]"
        )
        List<String> imageUrls
) {}
