package com.emomap.emomap.post.repository;              // 포스트 리포지토리 패키지

import com.emomap.emomap.post.entity.Post;              // 엔티티
import org.springframework.data.domain.*;               // 페이지네이션
import org.springframework.data.jpa.repository.*;       // @Query 등
import org.springframework.data.repository.query.Param; // 파라미터 바인딩
import java.util.List;                                  // 리스트 반환용

public interface PostRepository extends JpaRepository<Post, Long> { // CRUD + 쿼리

    @Query("select p from Post p order by p.createdAt desc")        // 최신순 피드
    Page<Post> findLatest(Pageable pageable);                       // Page로 받기

    // 검색/필터(키워드, 태그, 지도 범위) + 페이징
    @Query(
            value = """
            SELECT *
            FROM posts p
            WHERE
              -- q: 내용/주소 검색(없으면 무시)
              ( :q IS NULL OR :q = '' 
                OR p.content ILIKE CONCAT('%', :q, '%')
                OR p.road_address ILIKE CONCAT('%', :q, '%') )
              AND
              -- tag: emotions 포함 여부(없으면 무시)
              ( :tag IS NULL OR :tag = '' 
                OR p.emotions ILIKE CONCAT('%', :tag, '%') )
              AND
              -- 지도 범위(BBox): 네 값이 모두 있을 때만 적용
              ( (:minLat IS NULL OR :maxLat IS NULL OR :minLng IS NULL OR :maxLng IS NULL)
                OR (p.lat BETWEEN :minLat AND :maxLat AND p.lng BETWEEN :minLng AND :maxLng) )
            ORDER BY p.created_at DESC
            """,
            countQuery = """
            SELECT count(*)
            FROM posts p
            WHERE
              ( :q IS NULL OR :q = '' 
                OR p.content ILIKE CONCAT('%', :q, '%')
                OR p.road_address ILIKE CONCAT('%', :q, '%') )
              AND
              ( :tag IS NULL OR :tag = '' 
                OR p.emotions ILIKE CONCAT('%', :tag, '%') )
              AND
              ( (:minLat IS NULL OR :maxLat IS NULL OR :minLng IS NULL OR :maxLng IS NULL)
                OR (p.lat BETWEEN :minLat AND :maxLat AND p.lng BETWEEN :minLng AND :maxLng) )
            """,
            nativeQuery = true
    )
    Page<Post> searchNative(
            @Param("q") String q,
            @Param("tag") String tag,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            Pageable pageable
    );

    // 지도 마커 전용
    @Query(value = """
    SELECT p.id, p.lat, p.lng, p.emotions
    FROM posts p
    WHERE p.lat BETWEEN :minLat AND :maxLat
      AND p.lng BETWEEN :minLng AND :maxLng
    """, nativeQuery = true)
    List<MarkerView> findMarkersNative(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );
}
