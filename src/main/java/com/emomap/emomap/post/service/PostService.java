package com.emomap.emomap.post.service;

import com.emomap.emomap.post.entity.Post;
import com.emomap.emomap.post.entity.dto.request.CreatePostRequestDTO;
import com.emomap.emomap.post.entity.dto.response.MarkerResponseDTO;
import com.emomap.emomap.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {

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

    // 마커 목록 조회
    public List<MarkerResponseDTO> getMarkers(double swLat, double swLng, double neLat, double neLng) {
        return postRepository.findMarkers(swLat, neLat, swLng, neLng).stream()
                .map(p -> new MarkerResponseDTO(p.getId(), p.getLat(), p.getLng()))
                .toList();
    }
}
