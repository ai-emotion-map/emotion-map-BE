package com.emomap.emomap.post.entity.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePostRequestDTO(
        @NotNull Long userId,
        @NotBlank String content,
        String emotions,
        @DecimalMin("-90.0")  @DecimalMax("90.0")  double lat,
        @DecimalMin("-180.0") @DecimalMax("180.0") double lng,
        String roadAddress
) { }
