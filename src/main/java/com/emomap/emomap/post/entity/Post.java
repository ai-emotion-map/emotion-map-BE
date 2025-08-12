package com.emomap.emomap.post.entity;                  // 포스트 엔티티 패키지

import jakarta.persistence.*;                            // JPA 어노테이션
import lombok.*;                                         // lombok
import java.time.OffsetDateTime;                         // 시간 타입

@Entity                                                 // JPA 엔티티
@Table(                                                  // 테이블 설정
        name = "posts",                                 // 테이블명 posts 함
        indexes = {                                     // 자주 쓰는 조회 인덱스 박아두기
                @Index(name = "idx_posts_created_at", columnList = "createdAt"), // 최신순 정렬용
                @Index(name = "idx_posts_lat_lng", columnList = "lat,lng")       // 지도 범위 조회용
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder // lombok 세트
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // identity 사용
    private Long id;                                     // 글 pk

    @Column(name="user_id") // DB에서 이 컬럼명은 user_id로하고 Post와 User FK 연결함
    private Long userId;

    @Column(nullable=false, columnDefinition="text")     // null 값 안되고 text 타입임
    private String content;

    private String emotions;                             // ai 감정 분류 하여 "frendship" 같은 문자열

    @Column(nullable=false) private double lat;          // 위도
    @Column(nullable=false) private double lng;          // 경도

    private String roadAddress;                          // 도로명 주소

    @Column(nullable=false)
    private OffsetDateTime createdAt;                    // 생성 시각

    private OffsetDateTime updatedAt;                    // 수정 시각

    @PrePersist                                          // INSERT 전에 자동으로 호출
    void onCreate() {
        this.createdAt = OffsetDateTime.now();           // createdAt 안 들어왔으면 현재 시간으로 채워줌
    }
    @PreUpdate                                           // UPDATE 전에 자동 호출
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();           // 수정할 때마다 갱신해줌
    }
}
