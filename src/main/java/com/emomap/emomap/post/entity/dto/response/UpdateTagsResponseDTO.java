package com.emomap.emomap.post.entity.dto.response;

import java.util.List;

public record UpdateTagsResponseDTO(
        Long id,
        List<String> tags
) {}