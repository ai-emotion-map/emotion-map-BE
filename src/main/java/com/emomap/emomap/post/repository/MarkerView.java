package com.emomap.emomap.post.repository;

import java.time.LocalDateTime;

public interface MarkerView {
    Long getId();
    Double getLat();
    Double getLng();
    String getEmotions(); // 컨트롤러 및 서비스에서 split 해서 tags[]로 변환
    String getPlaceName();
    String getRoadAddress();
    String getContent();
    LocalDateTime getCreatedAt();
}
