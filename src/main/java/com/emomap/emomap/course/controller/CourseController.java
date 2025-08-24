package com.emomap.emomap.course.controller;

import com.emomap.emomap.course.entity.dto.CourseRequestDTO;
import com.emomap.emomap.course.entity.dto.CourseResponseDTO;
import com.emomap.emomap.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/emotions")
    public String[] emotions() {
        return new String[]{
                "가족","우정","위로/치유","외로움","설렘/사랑","향수","기쁨/신남","화남/분노"
        };
    }

    @Operation(
            summary = "감정 기반 코스 추천 (성북구 고정, 3개만 추천, 감정별 규칙)",
            description = """
            요청(JSON) 예:
            { "emotion": "화남/분노" }
            - 프론트에서 emotion 하나만 보내면 됨.
            - count=3, area=성북구 등은 서버에서 고정해놨음. 데이터에 따라 코스를 3개보다 적게 추천해줄 수도 있음.
            응답: stops(순서=코스), polyline([[lat,lng], ...]), 총거리/도보 시간 포함.
            """
    )
    @PostMapping("/recommend")
    public CourseResponseDTO recommend(@RequestBody CourseRequestDTO request) {
        return courseService.recommend(request);
    }
}
