package com.emomap.emomap.user.controller;              // 유저 컨트롤러 패키지

import com.emomap.emomap.user.entity.User;              // 엔티티
import com.emomap.emomap.user.entity.dto.request.LoginRequestDTO;
import com.emomap.emomap.user.entity.dto.request.SignupRequestDTO;
import com.emomap.emomap.user.entity.dto.response.JwtTokenResponseDTO;
import com.emomap.emomap.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;                   // 생성자 자동으로 만들어주는 lombok
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;        // @RestController, 매핑 어노테이션들

import java.util.Map;                                    // 간단 응답용 Map

@RestController                                          // REST API를 제공함
@RequiredArgsConstructor                                 // final 필드로 생성자 자동 주입
@RequestMapping("/api/user")                                // /users로 시작
public class UserController {
    private final UserService userService;                   // 의존성 추가

    // 회원가입
    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequestDTO req){
        userService.signupUser(req);
        return ResponseEntity.ok("회원가입 성공");
    }

    // 로그인
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<JwtTokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO req){
        return ResponseEntity.ok(userService.login(req));
    }
}