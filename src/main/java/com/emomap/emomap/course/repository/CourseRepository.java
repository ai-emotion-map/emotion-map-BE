package com.emomap.emomap.course.repository;

import com.emomap.emomap.post.entity.Post;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Post, Long> {

    @Query(value = """
        SELECT *
        FROM posts p
        WHERE p.road_address ILIKE CONCAT('서울특별시 ', :gu, '%%')
          AND (:tag IS NULL OR :tag = '' OR p.emotions ILIKE CONCAT('%%', :tag, '%%'))
          AND p.place_name IS NOT NULL
          AND p.lat IS NOT NULL AND p.lng IS NOT NULL
        ORDER BY p.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Post> findRecentInGuByTag(
            @Param("gu") String gu,
            @Param("tag") String tag,
            @Param("limit") int limit
    );
}
