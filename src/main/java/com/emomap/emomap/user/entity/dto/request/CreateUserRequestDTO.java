package com.emomap.emomap.user.entity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequestDTO(
        @Email @NotBlank String email,
        @NotBlank String nickname
) { }
