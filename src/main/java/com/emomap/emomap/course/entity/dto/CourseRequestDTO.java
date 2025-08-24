package com.emomap.emomap.course.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseRequestDTO(
        @Schema(description = "감정", example = "우정")
        String emotion
) {}
