package com.emomap.emomap.course.service;

import com.emomap.emomap.course.entity.dto.CourseRequestDTO;
import com.emomap.emomap.course.entity.dto.CourseResponseDTO;
import com.emomap.emomap.course.entity.dto.CourseStopDTO;
import com.emomap.emomap.course.repository.CourseRepository;
import com.emomap.emomap.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    /* ===== 고정값 ===== */
    private static final String AREA_DEFAULT = "성북구";
    private static final int COUNT_FIXED = 3;
    private static final int CANDIDATE_LIMIT = 200;

    /* ===== 감정/카테고리 정의 ===== */
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

    // 키워드 사전
    private static final String[] KW_WALK = {
            "산책","공원","둘레길","천","정릉천","성북천","성곽","한양도성","숲","북서울꿈의숲",
            "전시","미술관","박물관","도서관","포토","포토스팟","길","야외","산","녹지"
    };
    private static final String[] KW_VIEW = {
            "전망","뷰","전망대","야경","루프탑","스카이","언덕","성곽","전망 좋은"
    };
    private static final String[] KW_CAFE = {
            "카페","커피","디저트","브런치","베이커리","빵집","케이크","라떼","티룸","와플","스콘","마카롱"
    };
    private static final String[] KW_SHOP = {
            "소품","문구","편집샵","편집숍","리빙샵","마켓","플리마켓","빈티지","서점","북카페","플라워","꽃집","샵"
    };
    private static final String[] KW_FOOD = {
            "맛집","식당","고기","파스타","피자","초밥","라멘","국수","한식","분식","카레","버거",
            "매운","마라","닭발","떡볶이","곱창","닭갈비","치킨","전골","찜","칼국수","냉면"
    };
    private static final String[] KW_PUB = {
            "술집","호프","포차","주점","포장마차","펍","pub","izakaya","이자카야","bar","바","와인바",
            "칵테일","수제맥주","맥주","소주","하이볼"
    };
    private static final String[] KW_ACTIVITY = {
            "사격","양궁","볼링","당구","포켓볼","다트","방탈출","만화카페","노래방","코인노래방","오락실",
            "PC방","게임","클라이밍","스크린골프","스크린야구","탁구","펀치","타격","암벽"
    };

    // 감정별 시퀀스로 OR 그룹이며 해당 Kind 중 현재 기준점과 가장 가까운 후보 하나만 뽑는 것임
    // 예: 화남/분노 = [ACTIVITY] -> [PUB or FOOD] -> [WALK or VIEW]
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
                    List.of(Kind.ACTIVITY),          // 스트레스 해소
                    List.of(Kind.PUB, Kind.FOOD),    // 술집 or 매운 맛집
                    List.of(Kind.WALK, Kind.VIEW)    // 쿨다운
            )
    );

    public CourseResponseDTO recommend(CourseRequestDTO req) {
        // 1. 감정 정규화
        String emotion = normalizeEmotion(req.emotion());

        // 2. 지역과 감정 가져오기
        List<Post> candidates = courseRepository.findRecentInGuByTag(AREA_DEFAULT, emotion, CANDIDATE_LIMIT);
        if (candidates.isEmpty()) {
            return new CourseResponseDTO(emotion, AREA_DEFAULT, 0, 0.0, 0, List.of(), List.of());
        }

        // 3. placeName 중복 제거(최신 우선)
        candidates = dedupByPlaceName(candidates);

        // 4. 키워드로 Kind 분리
        Map<Kind, List<Post>> pools = buildPools(candidates);

        // 5. 감정 시퀀스에 따라 3곳 선택 (부족하면 다른 풀이나 전체에서 채움)
        // 기준점은 후보들의 중심으로 시작함. 이유는 코스가 성북구 내에서 너무 벌어지지 않게하기 위해서
        List<Post> picked = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        double[] centroid = centroid(candidates);
        double curLat = centroid[0], curLng = centroid[1];

        List<List<Kind>> seq = EMOTION_SEQUENCE.getOrDefault(
                emotion,
                List.of(
                        List.of(Kind.WALK, Kind.CAFE, Kind.ACTIVITY),
                        List.of(Kind.CAFE, Kind.FOOD, Kind.SHOP),
                        List.of(Kind.SHOP, Kind.VIEW, Kind.FOOD)
                )
        );
        // 각 그룹에서 현재 위치에서 제일 가까운 후보 하나만 선택
        for (List<Kind> group : seq) {
            Post p = pickNearestFromGroup(group, pools, curLat, curLng, used);
            if (p != null) {
                picked.add(p);
                used.add(p.getId());
                curLat = p.getLat();
                curLng = p.getLng();
            }
            if (picked.size() >= COUNT_FIXED) break;
        }

        if (picked.size() < COUNT_FIXED) {
            List<Post> rest = candidates.stream().filter(po -> !used.contains(po.getId())).toList();
            while (picked.size() < COUNT_FIXED && !rest.isEmpty()) {
                int idx = nearestIndex(rest, curLat, curLng);
                Post p = rest.get(idx);
                picked.add(p); used.add(p.getId());
                curLat = p.getLat(); curLng = p.getLng();
                rest = rest.stream().filter(po -> !used.contains(po.getId())).toList();
            }
        }

        // 6. polyline(stop만 연결)이랑 총거리 계산해서 도보시간 환산
        List<double[]> polyline = new ArrayList<>();
        List<CourseStopDTO> stops = new ArrayList<>();
        double totalKm = 0.0;

        for (int i = 0; i < picked.size(); i++) {
            Post p = picked.get(i);
            polyline.add(new double[]{p.getLat(), p.getLng()});

            // 거리: 하버사인(직선)
            double d = (i == 0) ? 0.0 : haversineKm(
                    picked.get(i - 1).getLat(), picked.get(i - 1).getLng(),
                    p.getLat(), p.getLng()
            );
            totalKm += d;

            String thumb = (p.getImageUrls()!=null && !p.getImageUrls().isEmpty()) ? p.getImageUrls().get(0) : null;

            stops.add(new CourseStopDTO(
                    p.getId(),
                    p.getPlaceName(),
                    p.getRoadAddress(),
                    p.getLat(),
                    p.getLng(),
                    splitKoTags(p.getEmotions()),
                    thumb,
                    safe(p.getContent()),
                    round1(d)
            ));
        }

        int walkMin = (int)Math.round(totalKm / 4.0 * 60.0); // 4km/h 기준
        return new CourseResponseDTO(emotion, AREA_DEFAULT, stops.size(), round1(totalKm), walkMin, polyline, stops);
    }

    /* ===== Utils ===== */

    private String normalizeEmotion(String in) {
        if (in == null) return "우정";
        String s = in.trim();
        if (EMOTIONS_KO.contains(s)) return s;
        String m = EN2KO.get(s.toLowerCase(Locale.ROOT));
        return (m != null) ? m : "우정";
    }

    private Map<Kind, List<Post>> buildPools(List<Post> cands) {
        return Map.of(
                Kind.WALK,     filterByKeywords(cands, KW_WALK),
                Kind.VIEW,     filterByKeywords(cands, KW_VIEW),
                Kind.CAFE,     filterByKeywords(cands, KW_CAFE),
                Kind.SHOP,     filterByKeywords(cands, KW_SHOP),
                Kind.FOOD,     filterByKeywords(cands, KW_FOOD),
                Kind.PUB,      filterByKeywords(cands, KW_PUB),
                Kind.ACTIVITY, filterByKeywords(cands, KW_ACTIVITY)
        );
    }

    private List<Post> filterByKeywords(List<Post> cands, String[] kws) {
        return cands.stream().filter(p -> {
            String target = (safe(p.getPlaceName()) + " " + safe(p.getContent())).toLowerCase(Locale.ROOT);
            for (String kw : kws) if (target.contains(kw.toLowerCase(Locale.ROOT))) return true;
            return false;
        }).toList();
    }

    private Post pickNearestFromGroup(List<Kind> group, Map<Kind, List<Post>> pools,
                                      double lat, double lng, Set<Long> used) {
        Post best = null; double bestD = Double.MAX_VALUE;
        for (Kind k : group) {
            List<Post> pool = pools.getOrDefault(k, List.of());
            for (Post p : pool) {
                if (used.contains(p.getId())) continue;
                double d = haversineKm(lat, lng, p.getLat(), p.getLng());
                if (d < bestD) { bestD = d; best = p; }
            }
        }
        return best;
    }

    private List<Post> dedupByPlaceName(List<Post> list) {
        Set<String> seen = new HashSet<>();
        List<Post> out = new ArrayList<>();
        for (Post p : list) {
            String key = safe(p.getPlaceName()).toLowerCase(Locale.ROOT);
            if (key.isEmpty()) continue;
            if (seen.add(key)) out.add(p);
        }
        return out;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static double[] centroid(List<Post> posts) {
        double lat = posts.stream().mapToDouble(Post::getLat).average().orElse(37.607);
        double lng = posts.stream().mapToDouble(Post::getLng).average().orElse(127.02);
        return new double[]{lat, lng};
    }

    private static int nearestIndex(List<Post> list, double lat, double lng) {
        int bestIdx = 0; double bestD = Double.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            Post p = list.get(i);
            double d = haversineKm(lat, lng, p.getLat(), p.getLng());
            if (d < bestD) { bestD = d; bestIdx = i; }
        }
        return bestIdx;
    }

    private static List<String> splitKoTags(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        Set<String> allow = EMOTIONS_KO;
        return Arrays.stream(csv.split("[,\\s]+"))
                .map(String::trim)
                .filter(allow::contains)
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
    }
    
    // 하버사인 공식으로 단위는 km임. 직선거리로만 계산함
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
}
