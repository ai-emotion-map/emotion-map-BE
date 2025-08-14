package com.emomap.emomap.post.controller;

import com.emomap.emomap.post.entity.Post;
import com.emomap.emomap.post.entity.dto.request.CreatePostRequestDTO;
import com.emomap.emomap.post.entity.dto.response.MarkerResponseDTO;
import com.emomap.emomap.post.repository.PostRepository;
import com.emomap.emomap.post.service.Emotion;
import com.emomap.emomap.post.service.Kakao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostRepository posts;
    private final Emotion emotionClassifier;
    private final Kakao kakaoAPI;

    @PostMapping
    public Map<String, Object> create(@RequestBody @Valid CreatePostRequestDTO req) {
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

        posts.save(p);
        return Map.of("id", p.getId());
    }

    @GetMapping("/{id}")
    public Post detail(@PathVariable Long id) {
        return posts.findById(id).orElseThrow();
    }

    @GetMapping("/latest")
    public Page<Post> latest(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return posts.findLatest(PageRequest.of(page, size));
    }

    @GetMapping("/markers")
    public List<MarkerResponseDTO> markers(@RequestParam double swLat, @RequestParam double swLng,
                                           @RequestParam double neLat, @RequestParam double neLng) {
        return posts.findMarkers(swLat, neLat, swLng, neLng).stream()
                .map(p -> new MarkerResponseDTO(p.getId(), p.getLat(), p.getLng()))
                .toList();
    }
}
