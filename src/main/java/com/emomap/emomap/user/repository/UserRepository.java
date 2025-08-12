package com.emomap.emomap.user.repository;              // 유저 리포지토리 패키지

import com.emomap.emomap.user.entity.User;              // 유저 엔티티 임포트
import org.springframework.data.jpa.repository.JpaRepository; // 스프링 데이터 JPA 기본 리포

public interface UserRepository extends JpaRepository<User, Long> {} // 기본 CRUD 다 제공됨.
