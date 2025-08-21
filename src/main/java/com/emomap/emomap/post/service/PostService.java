package com.emomap.emomap.post.service;

import com.emomap.emomap.post.entity.Post;
import com.emomap.emomap.post.entity.dto.response.CreatePostResponseDTO;
import com.emomap.emomap.post.entity.dto.response.FeedItemDTO;
import com.emomap.emomap.post.entity.dto.response.PostDetailResponseDTO;
import com.emomap.emomap.post.entity.dto.response.SearchPostResponseDTO;
import com.emomap.emomap.post.entity.dto.request.CreatePostRequestDTO;
import com.emomap.emomap.post.entity.dto.request.CreatePostFormDTO;
import com.emomap.emomap.post.repository.MarkerView;
import com.emomap.emomap.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import java.time.ZoneId;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PostService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final PostRepository postRepository;
    private final Emotion emotionClassifier;
    private final Kakao kakaoAPI;

    // 게시글 생성
    public Map<String, Object> createPost(CreatePostRequestDTO req) {
        String emo = emotionClassifier.classifyIfBlank(req.content(), req.emotions());

        String road = (req.roadAddress() == null || req.roadAddress().isBlank())
                ? kakaoAPI.findRoadAddress(req.lat(), req.lng()).orElse(null)
                : req.roadAddress();

        Post p = Post.builder()
                .userId(req.userId())
                .content(req.content())
                .emotions(emo)
                .lat(req.lat())
                .lng(req.lng())
                .roadAddress(road)
                .build();

        postRepository.save(p);
        return Map.of("id", p.getId());
    }

    // 게시글 상세 조회
    public Post getPostDetail(Long id) {
        return postRepository.findById(id).orElseThrow();
    }

    // 최신 게시글 페이지 조회
    public Page<Post> getLatestPosts(int page, int size) {
        return postRepository.findLatest(PageRequest.of(page, size));
    }

    // 검색/필터 + 페이징
    public Page<SearchPostResponseDTO> search(
            String q, String tag,
            Double minLat, Double maxLat,
            Double minLng, Double maxLng,
            int page, int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        Pageable pageable = PageRequest.of(page, size);

        Page<Post> result = postRepository.searchNative(
                emptyToNull(q),
                emptyToNull(tag),
                minLat, maxLat, minLng, maxLng,
                pageable
        );

        return result.map(this::toDto);
    }

    // 지도 마커 전용
    public List<SearchPostResponseDTO> markers(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        List<MarkerView> raw = postRepository.findMarkersNative(minLat, maxLat, minLng, maxLng);
        return raw.stream()
                .map(m -> new SearchPostResponseDTO(
                        m.getId(),
                        m.getLat(),
                        m.getLng(),
                        null,                 // 주소 및 내용 생략
                        null,
                        Arrays.asList(m.getEmotions().split(",")),
                        null
                ))
                .toList();
    }

    private SearchPostResponseDTO toDto(Post p) {
        return new SearchPostResponseDTO(
                p.getId(),
                p.getLat(),
                p.getLng(),
                p.getRoadAddress(),
                p.getContent(),
                splitTags(p.getEmotions()),
                toOffset(p.getCreatedAt())
        );
    }

    private List<String> splitTags(String emotions) {
        if (emotions == null || emotions.isBlank()) return List.of();
        return Arrays.stream(emotions.split("[,\\s]+"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    public CreatePostResponseDTO createPostForm(CreatePostFormDTO req) {
        // 1. 감정 및 주소 보정
        String emo = emotionClassifier.classifyIfBlank(req.content(), req.emotions());
        String road = (req.roadAddress() == null || req.roadAddress().isBlank())
                ? kakaoAPI.findRoadAddress(req.lat(), req.lng()).orElse(null)
                : req.roadAddress();

        // 2. 저장
        Post p = Post.builder()
                .userId(req.userId())
                .content(req.content())
                .emotions(emo)
                .lat(req.lat())
                .lng(req.lng())
                .roadAddress(road)
                .imageUrls(req.imageUrls()) // 컨트롤러에서 넘어온 imageUrls를 Post 엔티티에 저장
                .build();
        postRepository.save(p);

        // 3. 이미지 저장

        return new CreatePostResponseDTO(
                p.getId(), p.getLat(), p.getLng(), p.getRoadAddress(), splitTags(p.getEmotions())
        );
    }

    public PostDetailResponseDTO getPostDetailDto(Long id) {
        Post p = postRepository.findById(id).orElseThrow();

        // 이미지 URL 로딩(저장소가 있으면 교체)
        List<String> urls = p.getImageUrls();

        return new PostDetailResponseDTO(
                p.getId(), p.getLat(), p.getLng(), p.getRoadAddress(),
                p.getContent(), splitTags(p.getEmotions()), urls, toOffset(p.getCreatedAt())
        );
    }

    public Page<FeedItemDTO> getLatestFeed(int page, int size) {
        Page<Post> posts = postRepository.findLatest(PageRequest.of(page, size));
        return posts.map(p -> {
            String thumb = (p.getImageUrls() != null && !p.getImageUrls().isEmpty())
                    ? p.getImageUrls().get(0)
                    : null;
            return new FeedItemDTO(
                    p.getId(), p.getLat(), p.getLng(), p.getRoadAddress(),
                    thumb, splitTags(p.getEmotions()), toOffset(p.getCreatedAt())
            );
        });
    }

    private OffsetDateTime toOffset(java.time.LocalDateTime ldt) {
        return (ldt == null) ? null : ldt.atZone(KST).toOffsetDateTime();
    }
}
