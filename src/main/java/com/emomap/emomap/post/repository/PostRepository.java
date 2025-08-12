package com.emomap.emomap.post.repository;              // 포스트 리포지토리 패키지

import com.emomap.emomap.post.entity.Post;              // 엔티티
import org.springframework.data.domain.*;               // 페이지네이션
import org.springframework.data.jpa.repository.*;       // @Query 등
import org.springframework.data.repository.query.Param; // 파라미터 바인딩
import java.util.List;                                  // 리스트 반환용

public interface PostRepository extends JpaRepository<Post, Long> { // CRUD + 쿼리

    @Query("select p from Post p order by p.createdAt desc")        // 최신순 피드
    Page<Post> findLatest(Pageable pageable);                       // Page로 받기

    // 지도 마커 범위 조회
    @Query("""                                                      
    select p from Post p
     where p.lat between :swLat and :neLat
       and p.lng between :swLng and :neLng
     order by p.createdAt desc
  """)
    List<Post> findMarkers(@Param("swLat") double swLat,            // 남서쪽 위도
                           @Param("neLat") double neLat,            // 북동쪽 위도
                           @Param("swLng") double swLng,            // 남서쪽 경도
                           @Param("neLng") double neLng);           // 북동쪽 경도
}
