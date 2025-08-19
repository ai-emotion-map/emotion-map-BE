package com.emomap.emomap.common.jwt.service;

import com.emomap.emomap.common.jwt.JwtTokenUtil;
import com.emomap.emomap.common.jwt.entity.RefreshToken;
import com.emomap.emomap.common.jwt.repository.RefreshTokenRepository;
import com.emomap.emomap.user.entity.dto.response.JwtTokenResponseDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${spring.jwt.secretKey}")
    private String secretKey;

    @Value("${spring.jwt.expirationTime}")
    private Long accessExpirationTime;

    @Value("${spring.jwt.refresh-expirationTime}") // ← application.yml의 기존 키 그대로 사용
    private Long refreshExpirationTime;

    private final RefreshTokenRepository refreshRepo;

    // 1) 발급(+Redis 저장) — 로그인 때 사용
    public String issueAndStore(String email, String userAgent, String ip) {
        String jti = UUID.randomUUID().toString();
        String token = JwtTokenUtil.createRefreshToken(email, secretKey, refreshExpirationTime, jti);

        long ttlSec = refreshExpirationTime / 1000;
        refreshRepo.save(RefreshToken.builder()
                .jti(jti)
                .userEmail(email)
                .ttl(ttlSec)
                .build());

        return token;
    }

    // 2) 로테이션 — /auth/refresh
    public JwtTokenResponseDTO rotate(String refreshToken, String userAgent, String ip) {
        Claims claims = parse(refreshToken); // 서명/만료 검증 + 클레임 추출

        Object typ = claims.get("typ");
        if (!"refresh".equals(typ)) {
            throw new IllegalArgumentException("Not a refresh token");
        }

        String jti = claims.getId();
        String email = (String) claims.get("email");

        Optional<RefreshToken> saved = refreshRepo.findById(Long.valueOf(jti));
        if (saved.isEmpty()) {
            // 재사용/탈취 의심
            throw new IllegalStateException("Refresh token invalid or already used");
        }

        // 사용된 jti 제거(1회성)
        refreshRepo.deleteById(Long.valueOf(jti));

        String newAccess = JwtTokenUtil.createToken(email, secretKey, accessExpirationTime);

        // 3) 새 refresh 토큰 발급 & 저장
        String newRefresh = issueAndStore(email, userAgent, ip);

        // 4) 응답 DTO 구성 (필드가 있다면 함께 세팅)
        JwtTokenResponseDTO dto = new JwtTokenResponseDTO();
        dto.setAccess_token(newAccess);
        dto.setExpires_in(String.valueOf(accessExpirationTime));
        dto.setRefresh_token(newRefresh);
        dto.setRefresh_expires_in(String.valueOf(refreshExpirationTime));
        return dto;
    }

    // 3) 무효화 — 로그아웃
    public void revoke(String refreshToken) {
        Claims claims = parse(refreshToken);
        if (!"refresh".equals(claims.get("typ"))) return;

        refreshRepo.deleteById(Long.valueOf(claims.getId()));
    }

    // parser (jjwt 0.9.x)
    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}
