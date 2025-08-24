package com.emomap.emomap.course.entity.dto;

import java.util.List;

public record CourseStopDTO(
        String placeId,
        String placeName,
        String roadAddress,
        double lat,
        double lng,
        List<String> tags,   // ["우정","향수"] 등
        String thumbnailUrl, // 첫 이미지
        String content,             // 설명 (카카오 정보 요약)
        String kakaoUrl,            // 카카오 장소 상세 링크
        double distanceFromPrevKm   // 이전 stop에서 거리. 첫 stop=0
) {}
