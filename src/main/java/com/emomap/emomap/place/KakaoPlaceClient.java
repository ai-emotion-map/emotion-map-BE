package com.emomap.emomap.place;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KakaoPlaceClient {

    private final WebClient kakaoLocalClient;

    /* ===== Kakao 응답 DTO ===== */
    @Data
    public static class KakaoPlaceDoc {
        @JsonProperty("id")           String id;
        @JsonProperty("place_name")   String placeName;
        @JsonProperty("category_group_code") String categoryGroupCode;
        @JsonProperty("category_name")       String categoryName;
        @JsonProperty("phone")        String phone;
        @JsonProperty("address_name") String addressName;      // 지번
        @JsonProperty("road_address_name") String roadAddress; // 도로명
        @JsonProperty("x")            String x; // 경도
        @JsonProperty("y")            String y; // 위도
        @JsonProperty("place_url")    String placeUrl;
        @JsonProperty("distance")     String distance;
    }

    @Data
    public static class KakaoSearchResp {
        @JsonProperty("documents") List<KakaoPlaceDoc> documents;
        @JsonProperty("meta") Meta meta;
        @Data public static class Meta {
            @JsonProperty("is_end") boolean isEnd;
            @JsonProperty("pageable_count") int pageableCount;
            @JsonProperty("total_count") int totalCount;
        }
    }

    /* ===== 검색 파라미터 ===== */
    @Data
    public static class Rect {
        final double swLng, swLat, neLng, neLat;
        public String toParam() {
            return swLng + "," + swLat + "," + neLng + "," + neLat;
        }
    }

    /* ===== 키워드 검색 ===== */
    public List<KakaoPlaceDoc> searchKeywordInRect(String query, Rect rect, int size, int pages) {
        return fetchPaged("/v2/local/search/keyword.json", "query", query, rect, size, pages);
    }

    /* ===== 카테고리 검색 ===== */
    public List<KakaoPlaceDoc> searchCategoryInRect(String categoryCode, Rect rect, int size, int pages) {
        return fetchPaged("/v2/local/search/category.json", "category_group_code", categoryCode, rect, size, pages);
    }

    /* ===== 내부 공통 ===== */
    private List<KakaoPlaceDoc> fetchPaged(String path, String key, String value, Rect rect, int size, int pages) {
        java.util.ArrayList<KakaoPlaceDoc> out = new java.util.ArrayList<>();
        int page = 1;
        while (page <= pages) {
            final int pageParam = page;
            KakaoSearchResp resp = kakaoLocalClient.get()
                    .uri(uri -> uri.path(path)
                            .queryParam(key, value)
                            .queryParam("rect", rect.toParam())
                            .queryParam("page", pageParam)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .bodyToMono(KakaoSearchResp.class)
                    .block();

            if (resp == null || resp.getDocuments() == null) break;
            out.addAll(resp.getDocuments());
            if (resp.getMeta() == null || resp.getMeta().isEnd) break;
            page++;
        }
        return out;
    }
}
