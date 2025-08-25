package com.emomap.emomap.post.entity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record UpdateTagsRequestDTO(
        @Schema(description = "URL의 {id}와 같아야 함", example = "123")
        Long id,
        @Schema(description = "최종 확정 태그(1~3개, 허용 라벨만)", example = "[\"우정\",\"기쁨/신남\"]")
        List<String> tags
) {}