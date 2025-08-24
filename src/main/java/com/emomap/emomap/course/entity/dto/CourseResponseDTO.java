package com.emomap.emomap.course.entity.dto;

import java.util.List;

public record CourseResponseDTO(
        String emotion,             // 감정
        String area,                // "성북구" 고정
        int count,                  // 실제 추천되는 수(기본 3 목표)
        double totalDistanceKm,     // 총 직선거리
        int estimatedWalkMinutes,   // 4km/h 가정
        List<double[]> polyline,    // [[s1.lat,s1.lng],[s2.lat,s2.lng],[s3.lat,s3.lng]]
        List<CourseStopDTO> stops
) {}
