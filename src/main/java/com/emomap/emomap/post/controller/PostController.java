package com.emomap.emomap.post.controller;

import com.emomap.emomap.post.entity.dto.request.CreatePostFormDTO;
import com.emomap.emomap.post.entity.dto.response.CreatePostResponseDTO;
import com.emomap.emomap.post.entity.dto.response.FeedItemDTO;
import com.emomap.emomap.post.entity.dto.response.PostDetailResponseDTO;
import com.emomap.emomap.post.entity.dto.response.SearchPostResponseDTO;
import com.emomap.emomap.post.service.PostService;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "게시글 생성",
            description = """
                multipart/form-data: post(JSON 'post') + images(파일[] 선택)
                - 이미지가 없어도 사용 가능
                - AI가 감정 1~3개 자동 분류("가족","우정","위로/치유","외로움","설렘/사랑","향수","기쁨/신남","화남/분노" 이 것 중에서 분류)
                - 도로명 주소도 위,경도 가지고 kakao api가 자동 보정
                - post.placeName은 장소 이름(예: "스타벅스 종암점")
                """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CreatePostFormSwagger.class)
            )
    )
    @PostMapping(value = "/form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatePostResponseDTO> createByForm(
            @RequestPart("post") CreatePostFormDTO post,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity.ok(postService.createPostForm(post, images));
    }

    static class CreatePostFormSwagger {
        @Schema(
                description = """
                    게시글 JSON(이미지 없이 사용 가능, 감정 자동 분류):
                    {"content":"내용","lat":37.6,"lng":127.03,"placeName":"스타벅스 종암점"}
                    """,
                implementation = CreatePostFormDTO.class
        )
        public CreatePostFormDTO post;

        @ArraySchema(schema = @Schema(type = "string", format = "binary"))
        public List<MultipartFile> images;
    }

    @GetMapping("/{id}")
    public PostDetailResponseDTO detail(@PathVariable Long id) {
        return postService.getPostDetailDto(id);
    }

    @Operation(summary = "최신 피드", description = "가장 최근 게시글 페이지네이션")
    @GetMapping("/latest")
    public Page<FeedItemDTO> latest(@RequestParam(defaultValue="0") int page,
                                    @RequestParam(defaultValue="20") int size) {
        return postService.getLatestFeed(page, size);
    }

    @Operation(
            summary = "검색/필터",
            description = """
                    - q: 키워드(내용/주소/장소명)
                    - tag: 감정 태그(포함 검색, 한글만) 예: 우정, 향수
                    - 지도 범위: minLat, maxLat, minLng, maxLng (네 값 모두 있을 때만 적용)
                    - 페이지네이션: page(0부터), size(기본 20, 최대 100)
                    - 응답 필드: thumbnailUrl(첫 번째 이미지 URL, 없으면 null)
                    """
    )
    @GetMapping("/search")
    public Page<SearchPostResponseDTO> search(
            @Parameter(description = "키워드(내용/주소/장소명)") @RequestParam(required = false) String q,
            @Parameter(description = "감정 태그(포함 검색, 한글만)") @RequestParam(required = false) String tag,
            @Parameter(description = "최소 위도") @RequestParam(required = false) Double minLat,
            @Parameter(description = "최대 위도") @RequestParam(required = false) Double maxLat,
            @Parameter(description = "최소 경도") @RequestParam(required = false) Double minLng,
            @Parameter(description = "최대 경도") @RequestParam(required = false) Double maxLng,
            @Parameter(description = "페이지(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기(기본 20, 최대 100)") @RequestParam(defaultValue = "20") int size
    ) {
        return postService.search(q, tag, minLat, maxLat, minLng, maxLng, page, size);
    }

    @Operation(summary = "지도 마커 조회(경량)", description = "지도 범위(minLat,maxLat,minLng,maxLng) 없으면 전체")
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
