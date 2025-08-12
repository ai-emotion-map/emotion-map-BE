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

    // Kakao API 응답 구조에 맞춘 내부 클래스임
    @Data static class KakaoAddrDoc {
        @JsonProperty("road_address") Road road;     // JSON에서 "road_address" 필드를 Road 객체로 바꿈
        @Data static class Road {
            @JsonProperty("address_name") String addressName; // "address_name"을 addressName 필드로 바꿈
        }
    }
    // Kakao API 최상위 응답 구조
    @Data static class KakaoResp { List<KakaoAddrDoc> documents; }

    /* 좌표로 도로명주소 조회 */
    public Optional<String> findRoadAddress(double lat, double lng) {
        // WebClient로 카카오 로컬 API 호출
        KakaoResp resp = kakaoLocalClient.get()                 // GET 요청
                .uri(uri -> uri.path("/v2/local/geo/coord2address.json") // 좌표를 주소로 변환하는 API
                        .queryParam("x", lng) // Kakao는 x가 경도임
                        .queryParam("y", lat) // Kakao는 y가 위도임
                        .queryParam("input_coord", "WGS84") // 좌표계 지정
                        .build())
                .retrieve()                                    // 응답 가져오기
                .bodyToMono(KakaoResp.class)                   // JSON을 KakaoResp 매핑
                .block();                                      // 동기식으로 대기

        // 응답이 없거나 documents가 비었으면 Optional.empty로 반환
        if (resp == null || resp.getDocuments() == null || resp.getDocuments().isEmpty())
            return Optional.empty();

        // 첫 번째 결과에서 도로명 주소 꺼내서 Optional로 반환
        var road = resp.getDocuments().get(0).getRoad();
        return Optional.ofNullable(road).map(KakaoAddrDoc.Road::getAddressName);
    }
}
