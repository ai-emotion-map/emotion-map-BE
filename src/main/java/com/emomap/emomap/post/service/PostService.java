package com.emomap.emomap.post.service;

import com.emomap.emomap.post.entity.Post;
import com.emomap.emomap.post.entity.dto.request.CreatePostFormDTO;
import com.emomap.emomap.post.entity.dto.request.CreatePostRequestDTO;
import com.emomap.emomap.post.entity.dto.response.CreatePostResponseDTO;
import com.emomap.emomap.post.entity.dto.response.FeedItemDTO;
import com.emomap.emomap.post.entity.dto.response.PostDetailResponseDTO;
import com.emomap.emomap.post.entity.dto.response.SearchPostResponseDTO;
import com.emomap.emomap.post.repository.MarkerView;
import com.emomap.emomap.post.repository.PostRepository;
import com.emomap.emomap.post.service.StorageService;
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

    // JSON 생성
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

    // multipart/form-data 생성
    public CreatePostResponseDTO createPostForm(CreatePostFormDTO req, List<MultipartFile> images) {
        String content = req.content() == null ? "" : req.content().trim();
        String emoCsv  = emotionClassifier.classifyIfBlank(content, req.emotions());
        String road    = (req.roadAddress() == null || req.roadAddress().isBlank())
                ? kakaoAPI.findRoadAddress(req.lat(), req.lng()).orElse(null)
                : req.roadAddress();

        // 파일 저장 → URL 리스트
        List<String> imageUrls = Optional.ofNullable(images)
                .orElse(List.of())
                .stream()
                .filter(f -> f != null && !f.isEmpty() && f.getOriginalFilename() != null)
                .map(storageService::storeFile)
                .toList();

        Post p = Post.builder()
                .userId(req.userId())
                .content(content)
                .emotions(emoCsv)
                .lat(req.lat())
                .lng(req.lng())
                .roadAddress(road)
                .imageUrls(imageUrls)
                .build();

        postRepository.save(p);

        return new CreatePostResponseDTO(
                p.getId(), p.getLat(), p.getLng(), road, splitTags(emoCsv), imageUrls
        );
    }

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
                        null, null, splitTags(m.getEmotions()), null
                ))
                .toList();
    }

    private SearchPostResponseDTO toDto(Post p) {
        return new SearchPostResponseDTO(
                p.getId(), p.getLat(), p.getLng(),
                p.getRoadAddress(), p.getContent(),
                splitTags(p.getEmotions()), toOffset(p.getCreatedAt())
        );
    }

    private List<String> splitTags(String emotions) {
        if (emotions == null || emotions.isBlank()) return List.of();
        return Arrays.stream(emotions.split("[,\\s]+"))
                .filter(s -> !s.isBlank()).toList();
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private OffsetDateTime toOffset(java.time.LocalDateTime ldt) {
        return (ldt == null) ? null : ldt.atZone(KST).toOffsetDateTime();
    }

    public PostDetailResponseDTO getPostDetailDto(Long id) {
        Post p = postRepository.findById(id).orElseThrow();
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
                    ? p.getImageUrls().get(0) : null;
            return new FeedItemDTO(
                    p.getId(), p.getLat(), p.getLng(), p.getRoadAddress(),
                    thumb, splitTags(p.getEmotions()), toOffset(p.getCreatedAt())
            );
        });
    }
}
