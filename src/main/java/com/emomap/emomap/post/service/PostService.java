package com.emomap.emomap.post.service;

import com.emomap.emomap.post.entity.Post;
import com.emomap.emomap.post.entity.dto.request.CreatePostFormDTO;
import com.emomap.emomap.post.entity.dto.response.CreatePostResponseDTO;
import com.emomap.emomap.post.entity.dto.response.FeedItemDTO;
import com.emomap.emomap.post.entity.dto.response.PostDetailResponseDTO;
import com.emomap.emomap.post.entity.dto.response.SearchPostResponseDTO;
import com.emomap.emomap.post.repository.MarkerView;
import com.emomap.emomap.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PostRepository postRepository;
    private final Emotion emotionClassifier;
    private final Kakao kakaoAPI;
    private final StorageService storageService;

    private static final Set<String> KO_ALLOWED = Set.of(
            "가족","우정","위로/치유","외로움","설렘/사랑","향수"
    );

    /* ------------ 생성(FormData) ------------ */
    public CreatePostResponseDTO createPostForm(CreatePostFormDTO req, List<MultipartFile> images) {
        String content = req.content() == null ? "" : req.content().trim();

        // 1. 감정 태그 정규화(항상 한글 CSV)
        String emoCsv  = emotionClassifier.classifyIfBlank(content, req.emotions());

        // 2. 도로명 주소 보정
        String road    = (req.roadAddress() == null || req.roadAddress().isBlank())
                ? kakaoAPI.findRoadAddress(req.lat(), req.lng()).orElse(null)
                : req.roadAddress();

        // 3. 파일 저장은 URL 리스트로
        List<String> imageUrls = Optional.ofNullable(images)
                .orElse(List.of())
                .stream()
                .filter(f -> f != null && !f.isEmpty() && f.getOriginalFilename() != null)
                .map(storageService::storeFile)
                .toList();

        // 4. 저장
        Post p = Post.builder()
                .userId(req.userId())
                .content(content)
                .emotions(emoCsv)
                .lat(req.lat())
                .lng(req.lng())
                .roadAddress(road)
                .placeName(req.placeName())
                .imageUrls(imageUrls)
                .build();

        postRepository.save(p);

        // 5. 응답
        return new CreatePostResponseDTO(
                p.getId(), p.getLat(), p.getLng(), road, p.getPlaceName(), splitTags(p.getEmotions()), imageUrls
        );
    }

    /* -------------------- 조회/검색/피드 -------------------- */

    public Post getPostDetail(Long id) { return postRepository.findById(id).orElseThrow(); }

    public Page<Post> getLatestPosts(int page, int size) {
        return postRepository.findLatest(PageRequest.of(page, size));
    }

    public Page<SearchPostResponseDTO> search(String q, String tag,
                                              Double minLat, Double maxLat,
                                              Double minLng, Double maxLng,
                                              int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> result = postRepository.searchNative(
                emptyToNull(q), emptyToNull(tag), minLat, maxLat, minLng, maxLng, pageable
        );
        return result.map(this::toDto);
    }

    public List<SearchPostResponseDTO> markers(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        List<MarkerView> raw = postRepository.findMarkersNative(minLat, maxLat, minLng, maxLng);
        return raw.stream()
                .map(m -> new SearchPostResponseDTO(
                        m.getId(), m.getLat(), m.getLng(),
                        null, null, null, splitTags(m.getEmotions()), null
                ))
                .toList();
    }

    private SearchPostResponseDTO toDto(Post p) {
        return new SearchPostResponseDTO(
                p.getId(), p.getLat(), p.getLng(),
                p.getRoadAddress(), p.getPlaceName(), p.getContent(),
                splitTags(p.getEmotions()), toOffset(p.getCreatedAt())
        );
    }

    /* ------------------------ 유틸 ------------------------ */

    private List<String> splitTags(String emotions) {
        if (emotions == null || emotions.isBlank()) return List.of();
        return Arrays.stream(emotions.split("[,\\s]+"))
                .map(String::trim)
                .filter(KO_ALLOWED::contains)
                .distinct()
                .limit(3)
                .toList();
    }

    private String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    private OffsetDateTime toOffset(java.time.LocalDateTime ldt) {
        return (ldt == null) ? null : ldt.atZone(KST).toOffsetDateTime();
    }

    public PostDetailResponseDTO getPostDetailDto(Long id) {
        Post p = postRepository.findById(id).orElseThrow();
        return new PostDetailResponseDTO(
                p.getId(), p.getLat(), p.getLng(), p.getRoadAddress(), p.getPlaceName(),
                p.getContent(), splitTags(p.getEmotions()), p.getImageUrls(), toOffset(p.getCreatedAt())
        );
    }

    public Page<FeedItemDTO> getLatestFeed(int page, int size) {
        Page<Post> posts = postRepository.findLatest(PageRequest.of(page, size));
        return posts.map(p -> {
            String thumb = (p.getImageUrls() != null && !p.getImageUrls().isEmpty())
                    ? p.getImageUrls().get(0) : null;

            String preview = (p.getContent() != null && !p.getContent().isBlank())
                    ? p.getContent().substring(0, Math.min(50, p.getContent().length()))
                    : "";

            return new FeedItemDTO(
                    p.getId(),
                    p.getLat(),
                    p.getLng(),
                    p.getRoadAddress(),
                    p.getPlaceName(),
                    thumb,
                    splitTags(p.getEmotions()),
                    preview,
                    toOffset(p.getCreatedAt())
            );
        });
    }
}
