package com.emomap.emomap.user.service;

import com.emomap.emomap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class UserService {

    @Value("${spring.jwt.secretKey}") // jwt secretKey
    private String secretKey;
    @Value("${spring.jwt.expirationTime}")
    private Long expirationTime;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


}
