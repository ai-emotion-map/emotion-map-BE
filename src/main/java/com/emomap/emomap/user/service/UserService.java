package com.emomap.emomap.user.service;

import com.emomap.emomap.common.jwt.JwtTokenUtil;
import com.emomap.emomap.user.entity.User;
import com.emomap.emomap.user.entity.dto.request.LoginRequestDTO;
import com.emomap.emomap.user.entity.dto.request.SignupRequestDTO;
import com.emomap.emomap.user.entity.dto.response.JwtTokenResponseDTO;
import com.emomap.emomap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    //회원가입
    @Transactional
    public void signupUser(SignupRequestDTO req){
        if(checkEmailDuplication(req.getEmail()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 중복");
        if(!req.getPassword().equals(req.getCheckPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호 일치하지 않음");
        req.setPassword(passwordEncoder.encode(req.getPassword()));
        userRepository.save(User.builder()
                .email(req.getEmail())
                .password(req.getPassword())
                .nickname("user-" + RandomStringUtils.randomNumeric(6))
                .build());
    }

    //로그인
    @Transactional
    public JwtTokenResponseDTO login(LoginRequestDTO req){
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if(user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 찾을 수 없음");
        if(!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않음");

        JwtTokenResponseDTO res = new JwtTokenResponseDTO();
        res.setAccess_token(JwtTokenUtil.createToken(user.getEmail(), secretKey, expirationTime));
        res.setExpires_in(expirationTime.toString());
        return res;
    }

    //이메일 중복 확인
    public boolean checkEmailDuplication(String email) {
        return userRepository.existsByEmail(email);
    }

    //Id 조회
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    //Email 조회
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
