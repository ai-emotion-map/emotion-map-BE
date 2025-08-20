package com.emomap.emomap.post.entity.dto.response;

import java.util.List;

public record CreatePostResponseDTO(
        Long id,
        double lat,
        double lng,
        String roadAddress,
        List<String> tags
) {}