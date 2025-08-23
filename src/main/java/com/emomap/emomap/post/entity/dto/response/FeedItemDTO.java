package com.emomap.emomap.post.entity.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record FeedItemDTO(
        Long id,
        double lat,
        double lng,
        String roadAddress,
        String placeName,
        String thumbnailUrl,     // 처음 이미지(없으면 null임)
        List<String> tags,
        String content,
        OffsetDateTime createdAt
) {}