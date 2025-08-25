    package com.emomap.emomap.post.entity.dto.request;

    import java.util.List;

    public record CreatePostFormDTO(
            String content,
            Double lat,
            Double lng,
            String placeName,
            List<String> tags
    ) {}