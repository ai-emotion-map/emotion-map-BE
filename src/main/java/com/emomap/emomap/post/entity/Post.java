package com.emomap.emomap.post.entity;                  // 포스트 엔티티 패키지

import com.emomap.emomap.common.domain.BaseEntity;
import jakarta.persistence.*;                            // JPA 어노테이션
import lombok.*;                                         // lombok

import java.util.List;

@Entity                                                 // JPA 엔티티
@Table(                                                  // 테이블 설정
        name = "posts",                                 // 테이블명 posts 함
        indexes = {                                     // 자주 쓰는 조회 인덱스 박아두기
                @Index(name = "idx_posts_created_at", columnList = "created_at"), // 최신순 정렬용
                @Index(name = "idx_posts_lat_lng", columnList = "lat,lng")       // 지도 범위 조회용
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder // lombok 세트
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // identity 사용
    private Long id;                                     // 글 pk

    @Column(nullable=false, columnDefinition="text")     // null 값 안되고 text 타입임
    private String content;

    private String emotions;                             // ai 감정 분류 하여 "frendship" 같은 문자열

    @Column(nullable=false) private double lat;          // 위도
    @Column(nullable=false) private double lng;          // 경도

    private String roadAddress;                          // 도로명 주소

    @Column(name = "place_name")
    private String placeName;

    @ElementCollection
    @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "image_url")
    private List<String> imageUrls;
}
