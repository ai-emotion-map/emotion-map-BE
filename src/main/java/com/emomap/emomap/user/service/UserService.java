package com.emomap.emomap.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class MemberService {

    @Value("${spring.jwt.secretKey}") // jwt secretKey
    private String secretKey;
    @Value("${spring.jwt.expirationTime}")
    private Long expirationTime;


}
