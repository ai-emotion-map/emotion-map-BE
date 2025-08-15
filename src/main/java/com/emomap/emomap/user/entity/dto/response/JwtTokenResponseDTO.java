package com.emomap.emomap.user.entity.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JwtTokenResponseDTO {
    private String token_type  = "Bearer ";

    @NotBlank(message = "엑세스 토큰이 비어있습니다")
    private String access_token;

    @NotBlank(message = "엑세스 토큰 만료시간이 비어있습니다")
    private String expires_in;
}
