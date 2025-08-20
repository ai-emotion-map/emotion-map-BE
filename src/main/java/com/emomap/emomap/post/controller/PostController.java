package com.emomap.emomap.post.controller;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import com.emomap.emomap.post.entity.Post;
import com.emomap.emomap.post.entity.dto.request.CreatePostRequestDTO;
import com.emomap.emomap.post.entity.dto.response.CreatePostResponseDTO;
import com.emomap.emomap.post.entity.dto.response.FeedItemDTO;
import com.emomap.emomap.post.entity.dto.response.PostDetailResponseDTO;
import com.emomap.emomap.post.entity.dto.response.SearchPostResponseDTO;
import com.emomap.emomap.post.entity.dto.request.CreatePostFormDTO;
import com.emomap.emomap.post.service.PostService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성(JSON)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreatePostRequestDTO.class)
            )
    )

    @PostMapping(consumes = "application/json")
    public Map<String, Object> create(@RequestBody @Valid CreatePostRequestDTO req) {
        return postService.createPost(req);
    }


    @PostMapping(path = "/form", consumes = MULTIPART_FORM_DATA_VALUE)
    public CreatePostResponseDTO createByForm(
            @RequestPart("userId") Long userId,
            @RequestPart("content") String content,
            @RequestPart("lat") Double lat,
            @RequestPart("lng") Double lng,
            @RequestPart(value = "roadAddress", required = false) String roadAddress,
            @RequestPart(value = "emotions", required = false) String emotions,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        CreatePostFormDTO req = new CreatePostFormDTO(
                userId, content, lat, lng, roadAddress, emotions, images
        );
        return postService.createPostForm(req);
    }

    // 기존 상세 조회를 DTO로
    @GetMapping("/{id}")
    public PostDetailResponseDTO detail(@PathVariable Long id) {
        return postService.getPostDetailDto(id);
    }

    // 최신 글을 DTO 페이지로
    @GetMapping("/latest")
    public Page<FeedItemDTO> latest(@RequestParam(defaultValue="0") int page,
                                    @RequestParam(defaultValue="20") int size) {
        return postService.getLatestFeed(page, size);
    }


    @Operation(summary = "검색/필터",
            description = """
                    - q: 키워드(내용/주소)
                    - tag: 감정 태그(포함 검색)
                    - 지도 범위: minLat, maxLat, minLng, maxLng (네 값이 모두 있을 때만 적용)
                    - 페이지네이션: page(0부터), size(기본 20, 최대 100)
                    """)
    @GetMapping("/search")
    public Page<SearchPostResponseDTO> search(
            @Parameter(description = "키워드(내용/주소)") @RequestParam(required = false) String q,
            @Parameter(description = "감정 태그(포함 검색)") @RequestParam(required = false) String tag,
            @Parameter(description = "남서쪽 위도") @RequestParam(required = false) Double minLat,
            @Parameter(description = "북동쪽 위도") @RequestParam(required = false) Double maxLat,
            @Parameter(description = "남서쪽 경도") @RequestParam(required = false) Double minLng,
            @Parameter(description = "북동쪽 경도") @RequestParam(required = false) Double maxLng,
            @Parameter(description = "페이지(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기(기본 20, 최대 100)") @RequestParam(defaultValue = "20") int size
    ) {
        return postService.search(q, tag, minLat, maxLat, minLng, maxLng, page, size);
    }

    @Operation(summary = "지도 마커 조회(경량)",
            description = "지도 범위(minLat,maxLat,minLng,maxLng) 전달 시 해당 영역 보이게인데 없으면 전체임")
    @GetMapping("/markers")
    public List<SearchPostResponseDTO> markers(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLng
    ) {
        return postService.markers(minLat, maxLat, minLng, maxLng);
    }
}
