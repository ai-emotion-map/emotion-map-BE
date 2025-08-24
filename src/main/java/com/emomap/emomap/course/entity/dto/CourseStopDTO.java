package com.emomap.emomap.course.entity.dto;

import java.util.List;

public record CourseStopDTO(
        Long postId,
        String placeName,
        String roadAddress,
        double lat,
        double lng,
        List<String> tags,   // ["우정","향수"] 등
        String thumbnailUrl, // 첫 이미지
        String content,      // 전체 내용
        double distanceFromPrevKm // 이전 stop에서 거리. 첫 stop은 0
) {}
