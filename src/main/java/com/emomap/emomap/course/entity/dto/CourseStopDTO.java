package com.emomap.emomap.course.entity.dto;

import java.util.List;

public record CourseStopDTO(
        String placeId,
        String placeName,
        String roadAddress,
        double lat,
        double lng,
        List<String> tags,      // ["우정"] 등
        String content,         // 카카오 정보 요약(카테고리/전화/링크)
        String kakaoUrl
) {}