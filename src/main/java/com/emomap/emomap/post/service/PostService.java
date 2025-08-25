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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PostRepository postRepository;
    private final Emotion emotionClassifier;
    private final Kakao kakaoAPI;
    private final StorageService storageService;

    private static final Set<String> KO_ALLOWED = Set.of(
            "가족","우정","위로/치유","외로움","설렘/사랑","향수","기쁨/신남","화남/분노"
    );

    /* ------------ 생성(FormData) ------------ */
    public CreatePostResponseDTO createPostForm(CreatePostFormDTO req, List<MultipartFile> images) {
        String content = req.content() == null ? "" : req.content().trim();

        // 1. 감정 자동 분류
        String emoCsv;
        try {
            emoCsv = emotionClassifier.classifyIfBlank(content, null);
            emoCsv = normalizeKoCsv(emoCsv);
        } catch (Exception e) {
            log.warn("Emotion classify failed -> fallback empty. cause={}", e.toString());
            emoCsv = "";
        }
        log.info("[POST/FORM] classified tags csv='{}', placeName='{}', lat={}, lng={}",
                emoCsv, req.placeName(), req.lat(), req.lng());

        // 2. 도로명 주소 보정
        String road = kakaoAPI.findRoadAddress(req.lat(), req.lng()).orElse(null);

        // 3. 파일 저장
        List<String> imageUrls = Optional.ofNullable(images)
                .orElse(List.of())
                .stream()
                .filter(f -> f != null && !f.isEmpty() && f.getOriginalFilename() != null)
                .map(storageService::storeFile)
                .toList();

        // 4. 저장
        Post p = Post.builder()
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
        CreatePostResponseDTO dto = new CreatePostResponseDTO(
                p.getId(), p.getLat(), p.getLng(), road, p.getPlaceName(),
                splitTags(p.getEmotions()), imageUrls
        );
        log.info("[POST/FORM] saved id={}, tags={}, images={}",
                p.getId(), dto.tags(), dto.imageUrls().size());
        return dto;
    }

    private String normalizeKoCsv(String csv) {
        if (csv == null || csv.isBlank()) return "";
        List<String> cleaned = Arrays.stream(csv.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::mapToKoAllowed)
                .filter(KO_ALLOWED::contains)
                .distinct()
                .limit(3)
                .toList();
        return String.join(",", cleaned);
    }

    private String mapToKoAllowed(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.toLowerCase(Locale.ROOT);

        return switch (s) {
            case "family"        -> "가족";
            case "friend", "friends", "friendship" -> "우정";
            case "comfort", "healing" -> "위로/치유";
            case "lonely", "loneliness", "alone" -> "외로움";
            case "excitement", "excited", "flutter", "love", "heart" -> "설렘/사랑";
            case "nostalgia", "memory", "retro" -> "향수";
            default -> raw;
        };
    }

    /* -------------------- 조회/검색/피드 -------------------- */

    public Post getPostDetail(Long id) { return postRepository.findById(id).orElseThrow(); }

    public Page<Post> getLatestPosts(int page, int size) {
        return postRepository.findLatest(PageRequest.of(page, size));
    }

    private String firstOrNull(List<String> list) {
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
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
                        m.getId(),
                        m.getLat(),
                        m.getLng(),
                        m.getRoadAddress(),
                        m.getPlaceName(),
                        m.getContent(),
                        splitTags(m.getEmotions()),
                        toOffset(m.getCreatedAt()),
                        null
                ))
                .toList();
    }

    private SearchPostResponseDTO toDto(Post p) {
        return new SearchPostResponseDTO(
                p.getId(), p.getLat(), p.getLng(),
                p.getRoadAddress(), p.getPlaceName(), p.getContent(),
                splitTags(p.getEmotions()), toOffset(p.getCreatedAt()), firstOrNull(p.getImageUrls())
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

            return new FeedItemDTO(
                    p.getId(),
                    p.getLat(),
                    p.getLng(),
                    p.getRoadAddress(),
                    p.getPlaceName(),
                    thumb,
                    splitTags(p.getEmotions()),
                    p.getContent(),
                    toOffset(p.getCreatedAt())
            );
        });
    }

    // --- 태그만 수정 ---
    @Transactional
    public List<String> updateTagsAndReturn(Long id, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tags는 최소 1개 이상이어야 합니다.");
        }
        List<String> cleaned = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(KO_ALLOWED::contains)
                .distinct()
                .limit(3)
                .toList();
        if (cleaned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용된 라벨이 없습니다.");
        }

        var post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        post.setEmotions(String.join(",", cleaned));
        postRepository.save(post);


        return splitTags(post.getEmotions());
    }

}
