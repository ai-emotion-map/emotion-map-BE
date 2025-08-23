package com.emomap.emomap.post.entity.dto.request;

public record CreatePostFormDTO(
        String content,
        Double lat,
        Double lng,
        String roadAddress,          // 선택
        String placeName,
        String emotions           // 선택(비어 있으면 gemini로 분류함)
) {}