package com.emomap.emomap.course.service;

import com.emomap.emomap.course.entity.dto.CourseRequestDTO;
import com.emomap.emomap.course.entity.dto.CourseResponseDTO;
import com.emomap.emomap.course.entity.dto.CourseStopDTO;
import com.emomap.emomap.place.KakaoPlaceClient;
import com.emomap.emomap.place.KakaoPlaceClient.KakaoPlaceDoc;
import com.emomap.emomap.place.KakaoPlaceClient.Rect;
import com.emomap.emomap.place.PlaceLite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final KakaoPlaceClient kakao;

    private static final String AREA_DEFAULT = "성북구";
    private static final int COUNT_FIXED = 3;
    // 성북구 대략 bbox
    private static final Rect SB_RECT = new Rect(127.005, 37.586, 127.058, 37.634);

    private enum Kind { WALK, VIEW, CAFE, SHOP, FOOD, PUB, ACTIVITY }

    private static final Set<String> EMOTIONS_KO = Set.of(
            "가족","우정","위로/치유","외로움","설렘/사랑","향수","기쁨/신남","화남/분노"
    );
    private static final Map<String, String> EN2KO = Map.ofEntries(
            Map.entry("family","가족"),
            Map.entry("friendship","우정"),
            Map.entry("comfort","위로/치유"),
            Map.entry("lonely","외로움"),
            Map.entry("excitement","설렘/사랑"),
            Map.entry("nostalgia","향수"),
            Map.entry("joy","기쁨/신남"),
            Map.entry("anger","화남/분노")
    );

    // 감정별 코스 시퀀스(각 단계는 OR 그룹)
    private static final Map<String, List<List<Kind>>> EMOTION_SEQUENCE = Map.of(
            "우정", List.of(
                    List.of(Kind.WALK, Kind.ACTIVITY),
                    List.of(Kind.CAFE),
                    List.of(Kind.SHOP, Kind.FOOD)
            ),
            "향수", List.of(
                    List.of(Kind.WALK, Kind.VIEW),
                    List.of(Kind.CAFE),
                    List.of(Kind.SHOP)
            ),
            "설렘/사랑", List.of(
                    List.of(Kind.CAFE),
                    List.of(Kind.VIEW, Kind.WALK),
                    List.of(Kind.SHOP)
            ),
            "가족", List.of(
                    List.of(Kind.WALK),
                    List.of(Kind.FOOD),
                    List.of(Kind.CAFE)
            ),
            "위로/치유", List.of(
                    List.of(Kind.WALK),
                    List.of(Kind.CAFE),
                    List.of(Kind.VIEW)
            ),
            "외로움", List.of(
                    List.of(Kind.WALK),
                    List.of(Kind.VIEW),
                    List.of(Kind.CAFE)
            ),
            "기쁨/신남", List.of(
                    List.of(Kind.ACTIVITY),
                    List.of(Kind.FOOD),
                    List.of(Kind.CAFE)
            ),
            "화남/분노", List.of(
                    List.of(Kind.ACTIVITY),
                    List.of(Kind.PUB, Kind.FOOD),
                    List.of(Kind.WALK, Kind.VIEW)
            )
    );

    private static final Map<String, List<String>> LAST_RESULT_BY_EMOTION = new ConcurrentHashMap<>();

    private static final int TOP_K_NEAR = 5;


    public CourseResponseDTO recommend(CourseRequestDTO req) {
        String emotion = normalizeEmotion(req.emotion());

        Map<Kind, List<PlaceLite>> pools = fetchPoolsFromKakao();
        int totalCount = pools.values().stream().mapToInt(List::size).sum();
        if (totalCount == 0) {
            return new CourseResponseDTO(emotion, AREA_DEFAULT, 0, 0.0, 0, List.of(), List.of());
        }

        // 코스 선택(랜덤성 + 직전 결과 회피)
        List<PlaceLite> picked = pickCourseWithRandomness(emotion, pools);

        // 응답 매핑
        List<double[]> polyline = new ArrayList<>();
        List<CourseStopDTO> stops = new ArrayList<>();
        double totalKm = 0.0;

        for (int i = 0; i < picked.size(); i++) {
            PlaceLite p = picked.get(i);
            polyline.add(new double[]{p.getLat(), p.getLng()});

            // 구간 거리(총 거리 계산에만 사용)
            if (i > 0) {
                double segKm = round1(haversineKm(
                        picked.get(i - 1).getLat(), picked.get(i - 1).getLng(),
                        p.getLat(), p.getLng()
                ));
                totalKm += segKm;
            }

            String content = buildContentSummary(p);

            stops.add(new CourseStopDTO(
                    p.getId(),
                    p.getName(),
                    p.getRoadAddress(),
                    p.getLat(),
                    p.getLng(),
                    List.of(emotion),
                    content,
                    p.getKakaoUrl()
            ));
        }

        int walkMin = (int)Math.round(totalKm / 4.0 * 60.0);
        return new CourseResponseDTO(emotion, AREA_DEFAULT, stops.size(), round1(totalKm), walkMin, polyline, stops);
    }

    private List<PlaceLite> pickCourseWithRandomness(String emotion, Map<Kind, List<PlaceLite>> pools) {
        List<PlaceLite> picked = new ArrayList<>();
        Set<String> used = new HashSet<>();

        double[] centroid = centroidAll(pools);
        double curLat = centroid[0], curLng = centroid[1];

        List<List<Kind>> seq = EMOTION_SEQUENCE.getOrDefault(
                emotion,
                List.of(
                        List.of(Kind.WALK, Kind.CAFE, Kind.ACTIVITY),
                        List.of(Kind.CAFE, Kind.FOOD, Kind.SHOP),
                        List.of(Kind.SHOP, Kind.VIEW, Kind.FOOD)
                )
        );

        Random rnd = new Random(System.nanoTime());

        for (List<Kind> group : seq) {
            PlaceLite p = pickRandomFromTopK(group, pools, curLat, curLng, used, rnd);
            if (p != null) {
                picked.add(p); used.add(p.getId());
                curLat = p.getLat(); curLng = p.getLng();
            }
            if (picked.size() >= COUNT_FIXED) break;
        }

        // 부족분은 전체에서 근접 TOP_K_NEAR 중 랜덤으로 채움
        if (picked.size() < COUNT_FIXED) {

            List<PlaceLite> rest = pools.values().stream().flatMap(List::stream)
                    .filter(pl -> !used.contains(pl.getId()))
                    .toList();
            while (picked.size() < COUNT_FIXED && !rest.isEmpty()) {
                final double refLat = curLat;
                final double refLng = curLng;

                rest = rest.stream()
                        .sorted(Comparator.comparingDouble(pl ->
                                haversineKm(pl.getLat(), pl.getLng(), refLat, refLng))) // ← refLat/refLng 사용
                        .limit(TOP_K_NEAR)
                        .collect(Collectors.toList());

                PlaceLite p = rest.get(rnd.nextInt(rest.size()));
                picked.add(p); used.add(p.getId());

                curLat = p.getLat();
                curLng = p.getLng();

                rest = pools.values().stream().flatMap(List::stream)
                        .filter(pl -> !used.contains(pl.getId()))
                        .toList();
            }
        }

        // 직전과 동일 조합이면 마지막 하나 교체 시도
        List<String> ids = picked.stream().map(PlaceLite::getId).sorted().toList();
        List<String> last = LAST_RESULT_BY_EMOTION.getOrDefault(emotion, List.of());
        if (picked.size() == COUNT_FIXED && ids.equals(last)) {
            replaceLastWithDifferent(picked, pools, rnd);
        }
        LAST_RESULT_BY_EMOTION.put(emotion, picked.stream().map(PlaceLite::getId).sorted().toList());

        return picked;
    }

    private PlaceLite pickRandomFromTopK(List<Kind> group, Map<Kind, List<PlaceLite>> pools,
                                         double lat, double lng, Set<String> used, Random rnd) {
        List<PlaceLite> candidates = new ArrayList<>();
        for (Kind k : group) {
            for (PlaceLite p : pools.getOrDefault(k, List.of())) {
                if (!used.contains(p.getId())) candidates.add(p);
            }
        }
        if (candidates.isEmpty()) return null;

        candidates.sort(Comparator.comparingDouble(p -> haversineKm(lat, lng, p.getLat(), p.getLng())));
        int limit = Math.min(TOP_K_NEAR, candidates.size());
        return candidates.get(rnd.nextInt(limit));
    }

    private void replaceLastWithDifferent(List<PlaceLite> picked, Map<Kind, List<PlaceLite>> pools, Random rnd) {
        Set<String> used = picked.stream().map(PlaceLite::getId).collect(Collectors.toSet());
        List<PlaceLite> all = pools.values().stream().flatMap(List::stream)
                .filter(p -> !used.contains(p.getId()))
                .toList();
        if (!all.isEmpty()) {
            picked.set(picked.size() - 1, all.get(rnd.nextInt(all.size())));
        }
    }

    /* ===== Kakao 후보 수집 ===== */

    private Map<Kind, List<PlaceLite>> fetchPoolsFromKakao() {
        Map<Kind, List<PlaceLite>> map = new EnumMap<>(Kind.class);
        for (Kind k : Kind.values()) map.put(k, new ArrayList<>());

        // 카테고리
        map.get(Kind.CAFE).addAll(toLite(kakao.searchCategoryInRect("CE7", SB_RECT, 15, 3)));
        map.get(Kind.FOOD).addAll(toLite(kakao.searchCategoryInRect("FD6", SB_RECT, 15, 3)));
        map.get(Kind.PUB ).addAll(toLite(kakao.searchCategoryInRect("OL7", SB_RECT, 15, 2)));

        // 관광/문화
        List<KakaoPlaceDoc> culture = new ArrayList<>();
        culture.addAll(kakao.searchCategoryInRect("AT4", SB_RECT, 15, 2));
        culture.addAll(kakao.searchCategoryInRect("CT1", SB_RECT, 15, 2));

        // 키워드: Walk/View/Shop/Activity
        List<KakaoPlaceDoc> walkKw = new ArrayList<>();
        walkKw.addAll(kakao.searchKeywordInRect("산책로", SB_RECT, 15, 2));
        walkKw.addAll(kakao.searchKeywordInRect("성곽길", SB_RECT, 15, 1));
        walkKw.addAll(kakao.searchKeywordInRect("성북천", SB_RECT, 15, 1));
        walkKw.addAll(kakao.searchKeywordInRect("정릉천", SB_RECT, 15, 1));
        walkKw.addAll(kakao.searchKeywordInRect("공원", SB_RECT, 15, 2));

        List<KakaoPlaceDoc> viewKw = new ArrayList<>();
        viewKw.addAll(kakao.searchKeywordInRect("전망", SB_RECT, 15, 1));
        viewKw.addAll(kakao.searchKeywordInRect("야경", SB_RECT, 15, 1));
        viewKw.addAll(kakao.searchKeywordInRect("루프탑", SB_RECT, 15, 1));

        List<KakaoPlaceDoc> shopKw = new ArrayList<>();
        shopKw.addAll(kakao.searchKeywordInRect("소품샵", SB_RECT, 15, 2));
        shopKw.addAll(kakao.searchKeywordInRect("문구점", SB_RECT, 15, 1));
        shopKw.addAll(kakao.searchKeywordInRect("빈티지샵", SB_RECT, 15, 1));
        shopKw.addAll(kakao.searchKeywordInRect("서점", SB_RECT, 15, 1));
        shopKw.addAll(kakao.searchKeywordInRect("플라워", SB_RECT, 15, 1));

        List<KakaoPlaceDoc> actKw = new ArrayList<>();
        for (String q : List.of("볼링장","다트","사격","방탈출","노래방","오락실","클라이밍","스크린골프","스크린야구","만화카페","탁구")) {
            actKw.addAll(kakao.searchKeywordInRect(q, SB_RECT, 15, 1));
        }

        // 매핑
        map.get(Kind.WALK).addAll(toLite(walkKw));
        map.get(Kind.VIEW).addAll(toLite(viewKw));
        map.get(Kind.SHOP).addAll(toLite(shopKw));
        map.get(Kind.WALK).addAll(toLite(culture));
        map.get(Kind.VIEW).addAll(toLite(culture));
        map.get(Kind.ACTIVITY).addAll(toLite(actKw));

        // 중복 제거 + 트림
        map.replaceAll((k, v) -> dedupAndTrim(v, 80));
        return map;
    }

    private List<PlaceLite> toLite(List<KakaoPlaceDoc> docs) {
        return docs.stream()
                .filter(Objects::nonNull)
                .map(d -> {
                    double lat = parseDoubleSafe(d.getY());
                    double lng = parseDoubleSafe(d.getX());
                    return PlaceLite.builder()
                            .id(d.getId())
                            .name(nullToEmpty(d.getPlaceName()))
                            .roadAddress(nullToEmpty(
                                    (d.getRoadAddress()!=null && !d.getRoadAddress().isBlank())
                                            ? d.getRoadAddress() : d.getAddressName()
                            ))
                            .lat(lat)
                            .lng(lng)
                            .kakaoUrl(d.getPlaceUrl())
                            .phone(d.getPhone())
                            .categoryGroupCode(d.getCategoryGroupCode())
                            .categoryName(d.getCategoryName())
                            .build();
                })
                .filter(p -> p.getRoadAddress()!=null && p.getRoadAddress().contains("성북구"))
                .collect(Collectors.toList());
    }

    private List<PlaceLite> dedupAndTrim(List<PlaceLite> list, int limit) {
        Map<String, PlaceLite> uniq = new LinkedHashMap<>();
        for (PlaceLite p : list) uniq.putIfAbsent(p.getId(), p);
        return uniq.values().stream().limit(limit).toList();
    }

    private static double[] centroidAll(Map<Kind, List<PlaceLite>> pools) {
        List<PlaceLite> all = pools.values().stream().flatMap(List::stream).toList();
        double lat = all.stream().mapToDouble(PlaceLite::getLat).average().orElse(37.607);
        double lng = all.stream().mapToDouble(PlaceLite::getLng).average().orElse(127.02);
        return new double[]{lat, lng};
    }

    private static String buildContentSummary(PlaceLite p) {
        StringBuilder sb = new StringBuilder();
        if (p.getCategoryName()!=null && !p.getCategoryName().isBlank()) {
            sb.append("카테고리: ").append(p.getCategoryName());
        }
        if (p.getPhone()!=null && !p.getPhone().isBlank()) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("전화: ").append(p.getPhone());
        }
        if (p.getKakaoUrl()!=null && !p.getKakaoUrl().isBlank()) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("링크: ").append(p.getKakaoUrl());
        }
        return sb.isEmpty() ? "카카오 장소 정보" : sb.toString();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static double parseDoubleSafe(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; } }
    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng/2)*Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private String normalizeEmotion(String in) {
        if (in == null) return "우정";
        String s = in.trim();
        if (EMOTIONS_KO.contains(s)) return s;
        String m = EN2KO.get(s.toLowerCase(Locale.ROOT));
        return (m != null) ? m : "우정";
    }
}
