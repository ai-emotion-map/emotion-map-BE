package com.emomap.emomap.user.controller;              // 유저 컨트롤러 패키지

import com.emomap.emomap.user.repository.UserRepository; // DB 접근용 리포
import com.emomap.emomap.user.entity.User;              // 엔티티
import lombok.RequiredArgsConstructor;                   // 생성자 자동으로 만들어주는 lombok
import org.springframework.web.bind.annotation.*;        // @RestController, 매핑 어노테이션들

import java.util.Map;                                    // 간단 응답용 Map

@RestController                                          // REST API를 제공함
@RequiredArgsConstructor                                 // final 필드로 생성자 자동 주입
@RequestMapping("/api/user")                                // /users로 시작
public class UserController {
    private final UserRepository repo;                   // 의존성 추가

    @PostMapping                                         // POST /users로 유저 생성함
    public Map<String,Object> create(@RequestBody CreateUserReq req) { // 요청 바디를 DTO로 받음
        User u = User.builder().email(req.email()).nickname(req.nickname()).build(); // 엔티티로 매핑
        repo.save(u);                                   // DB에 저장
        return Map.of("id", u.getId());                 // 생성된 유저 id만 반환
    }

    @GetMapping("/{id}")                                 // GET /users/{id}로 조회
    public User get(@PathVariable Long id) {             // 경로에서 Long을 받음
        return repo.findById(id).orElseThrow();          // 없으면 404 오류 나옴
    }
}

// 요청 바디용 DTO
record CreateUserReq(String email, String nickname) {}   // 이메일이랑 닉네임만 받으면 됨
