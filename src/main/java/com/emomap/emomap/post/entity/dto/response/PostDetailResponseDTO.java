package com.emomap.emomap.post.entity.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record PostDetailResponseDTO(
        Long id,
        double lat,
        double lng,
        String roadAddress,
        String placeName,
        String content,
        List<String> tags,
        List<String> imageUrls,
        OffsetDateTime createdAt
) {}