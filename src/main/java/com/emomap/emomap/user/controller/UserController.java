package com.emomap.emomap.user.controller;

import com.emomap.emomap.common.jwt.service.RefreshTokenService;
import com.emomap.emomap.user.entity.dto.request.LoginRequestDTO;
import com.emomap.emomap.user.entity.dto.request.SignupRequestDTO;
import com.emomap.emomap.user.entity.dto.response.JwtTokenResponseDTO;
import com.emomap.emomap.user.repository.UserRepository; // DB 접근용 리포
import com.emomap.emomap.user.entity.User;              // 엔티티
import com.emomap.emomap.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;                   // 생성자 자동으로 만들어주는 lombok
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;        // @RestController, 매핑 어노테이션들

import java.util.Map;                                    // 간단 응답용 Map

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public record RefreshReq(String refreshToken) {}

    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequestDTO req){
        userService.signupUser(req);
        return ResponseEntity.ok("회원가입 성공");
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<JwtTokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO req){
        return ResponseEntity.ok(userService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenResponseDTO> refresh(@RequestBody RefreshReq req) {
        JwtTokenResponseDTO tokens = refreshTokenService.rotate(req.refreshToken(), null, null);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshReq req) {
        refreshTokenService.revoke(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
