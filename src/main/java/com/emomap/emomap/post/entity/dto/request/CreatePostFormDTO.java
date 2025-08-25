package com.emomap.emomap.post.entity.dto.request;

public record CreatePostFormDTO(
        String content,
        Double lat,
        Double lng,
        String placeName
) {}