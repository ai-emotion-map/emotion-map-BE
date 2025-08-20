package com.emomap.emomap.post.entity.dto.request;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public record CreatePostFormDTO(
        Long userId,
        String content,
        Double lat,
        Double lng,
        String roadAddress,          // 선택
        String emotions,             // 선택(비어 있으면 gemini로 분류함)
        List<String> imageUrls   // 선택 (여러 장)
) {}