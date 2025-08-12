package com.emomap.emomap.post.controller;             // 포스트 컨트롤러 패키지

import com.emomap.emomap.post.repository.PostRepository; // DB 접근 리포
import com.emomap.emomap.post.entity.Post;              // 엔티티
import com.emomap.emomap.post.service.Emotion; // GPT 감정 분류 서비스
import com.emomap.emomap.post.service.Kakao; // 카카오 API 서비스
import jakarta.validation.Valid;                        // 요청 DTO 검증
import lombok.RequiredArgsConstructor;                  // 생성자 주입 롬복
import org.springframework.data.domain.*;               // Page 관련
import org.springframework.web.bind.annotation.*;       // REST 매핑

import java.util.*;                                     // Map이나 리스트 등

@RestController                                         // REST API 컨트롤러
@RequiredArgsConstructor                                // final 필드 생성자 자동 생성
@RequestMapping("/posts")                               // /posts로 시작
public class PostController {
    private final PostRepository posts;                 // 리포
    private final Emotion emotionClassifier;  // 감정 분류기
    private final Kakao kakaoAPI;   // kakao

    @PostMapping                                        // POST /posts로 글 생성
    public Map<String,Object> create(@RequestBody @Valid CreatePostReq req) {
        // 1. 감정이 비어있으면 GPT로 분류해서 채우기
        String emo = emotionClassifier.classifyIfBlank(req.content(), req.emotions());

        // 2. 주소: roadAddress가 비어있으면 카카오 api로 채워넣기
        String road = (req.roadAddress()==null || req.roadAddress().isBlank())
                ? kakaoAPI.findRoadAddress(req.lat(), req.lng()).orElse(null)
                : req.roadAddress();

        // 엔티티로 매핑해서 저장
        Post p = Post.builder()
                .userId(req.userId())                  // FK
                .content(req.content())                // 본문
                .emotions(emo)                         // 감정 결과(문자열)
                .lat(req.lat()).lng(req.lng())         // 좌표
                .roadAddress(road)                     // 도로명 주소
                .build();

        posts.save(p);                                 // DB에 저장
        return Map.of("id", p.getId());                // 생성된 글 id만 반환
    }

    @GetMapping("/{id}")                                // GET /posts/{id}로 정보 확인
    public Post detail(@PathVariable Long id) {         // PK로 조회함
        return posts.findById(id).orElseThrow();
    }

    @GetMapping("/latest")                              // GET /posts/latest로 최신 피드를 조회
    public Page<Post> latest(@RequestParam(defaultValue="0") int page, // 페이지 번호를 0부터 보게
                             @RequestParam(defaultValue="20") int size) { // 페이지 크기를 20으로 지정
        return posts.findLatest(PageRequest.of(page, size)); // 최신순 페이지 반환해줌
    }

    @GetMapping("/markers")                             // GET /posts/markers로 지도 마커
    public List<MarkerRes> markers(@RequestParam double swLat, @RequestParam double swLng,
                                   @RequestParam double neLat, @RequestParam double neLng) {
        return posts.findMarkers(swLat, neLat, swLng, neLng).stream() // 범위에 해당하는 글 불러와서
                .map(p -> new MarkerRes(p.getId(), p.getLat(), p.getLng())) // 마커 DTO로 변환하여
                .toList();                             // 리스트로 반환함
    }
}

// 마커 응답 DTO
record MarkerRes(Long id, double lat, double lng) {}

// 글 생성 요청 DTO
record CreatePostReq(
        Long userId, // 글 쓴 사람 PK임. User 테이블의 id랑 매칭
        String content, // 글 내용
        String emotions, // 감정 태그.
        double lat, double lng, // 위도, 경도
        String roadAddress // 도로명 주소
) {}
