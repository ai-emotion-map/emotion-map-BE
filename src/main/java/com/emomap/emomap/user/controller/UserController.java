package com.emomap.emomap.user.controller;

import com.emomap.emomap.user.entity.User;
import com.emomap.emomap.user.entity.dto.request.CreateUserRequestDTO;
import com.emomap.emomap.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRepository repo;

    @PostMapping
    public Map<String, Object> create(@RequestBody @Valid CreateUserRequestDTO req) {
        User u = User.builder()
                .email(req.email())
                .nickname(req.nickname())
                .build();
        repo.save(u);
        return Map.of("id", u.getId());
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow();
    }
}
