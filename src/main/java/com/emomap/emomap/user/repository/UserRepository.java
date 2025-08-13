package com.emomap.emomap.user.repository;              // 유저 리포지토리 패키지

import com.emomap.emomap.user.entity.User;              // 유저 엔티티 임포트
import org.springframework.data.jpa.repository.JpaRepository; // 스프링 데이터 JPA 기본 리포

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
} // 기본 CRUD 다 제공됨.
