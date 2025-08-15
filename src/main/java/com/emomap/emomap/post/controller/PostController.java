package com.emomap.emomap.post.controller;

import com.emomap.emomap.post.entity.Post;
import com.emomap.emomap.post.entity.dto.request.CreatePostRequestDTO;
import com.emomap.emomap.post.entity.dto.response.MarkerResponseDTO;
import com.emomap.emomap.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public Map<String, Object> create(@RequestBody @Valid CreatePostRequestDTO req) {
        return postService.createPost(req);
    }

    @GetMapping("/{id}")
    public Post detail(@PathVariable Long id) {
        return postService.getPostDetail(id);
    }

    @GetMapping("/latest")
    public Page<Post> latest(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return postService.getLatestPosts(page, size);
    }

    @GetMapping("/markers")
    public List<MarkerResponseDTO> markers(@RequestParam double swLat, @RequestParam double swLng,
                                           @RequestParam double neLat, @RequestParam double neLng) {
        return postService.getMarkers(swLat, swLng, neLat, neLng);
    }
}
