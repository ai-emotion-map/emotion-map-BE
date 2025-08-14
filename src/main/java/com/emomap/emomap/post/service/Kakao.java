package com.emomap.emomap.post.service;   // 포스트 관련 서비스 패키지

import com.fasterxml.jackson.annotation.JsonProperty; // JSON 필드명 매핑용
import lombok.Data;                                   // lombok
import lombok.RequiredArgsConstructor;               // final 필드 생성자 주입
import org.springframework.stereotype.Service;       // 서비스 빈 등록
import org.springframework.web.reactive.function.client.WebClient; // HTTP 호출용 WebClient

import java.util.List;                                // 리스트 타입
import java.util.Optional;                            // 값이 있을 수도 없을 수도 있음

@Service
@RequiredArgsConstructor                             // final 필드 생성자 자동 주입
public class Kakao {

    private final WebClient kakaoLocalClient;        // 카카오 API 호출용 WebClient
    @Data static class KakaoAddrDoc {
        @JsonProperty("road_address") Road road;     // 도로명 주소
        @JsonProperty("address") Addr addr;          // 지번 주소

        @Data static class Road {
            @JsonProperty("address_name") String addressName; // "서초대로 77길 31" 같은 도로명 전체임
        }
        @Data static class Addr {
            @JsonProperty("address_name") String addressName; // "서울 강남구 역삼동 858" 같은 지번 전체임
        }
    }

    @Data static class KakaoResp {                   // 최상위 응답
        List<KakaoAddrDoc> documents;
    }

    /* 좌표로 주소를 조회시킴. 만약 도로명 없으면 지번으로 폴백해서 Optional<String> 반환 하게 함*/
    public Optional<String> findRoadAddress(double lat, double lng) {
        try {
            KakaoResp resp = kakaoLocalClient.get()
                    .uri(uri -> uri.path("/v2/local/geo/coord2address.json")
                            .queryParam("x", lng)               // Kakao는 x=경도
                            .queryParam("y", lat)               // Kakao는 y=위도
                            .queryParam("input_coord", "WGS84") // 좌표계
                            .build())
                    .retrieve()
                    // 4xx나 5xx면 응답 바디까지 가져와서 로그를 남기고 예외처리함
                    .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                            r -> r.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Kakao error: " + body)))
                    .bodyToMono(KakaoResp.class)
                    .block(); // 동기 호출

            if (resp == null || resp.getDocuments() == null || resp.getDocuments().isEmpty())
                return Optional.empty(); // 결과 자체가 없으면 빈 값

            var doc = resp.getDocuments().get(0);

            // 1순위로 도로명 주소가 나오게 하고 2순위로는 지번주소가 나오게 함
            String road  = doc.getRoad() != null ? doc.getRoad().getAddressName() : null;
            String jibun = doc.getAddr() != null ? doc.getAddr().getAddressName() : null;

            var chosen = (road != null && !road.isBlank()) ? road : jibun; // 도로명을 지번으로 폴백함
            return Optional.ofNullable(chosen);

        } catch (Exception e) {
            // 실패 하면 빈 값을 반환함.
            System.out.println("[Kakao 주소조회 실패] " + e.getMessage());
            return Optional.empty();
        }
    }
}
