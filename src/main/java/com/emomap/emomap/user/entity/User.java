package com.emomap.emomap.user.entity;                  // 유저 엔티티 패키지

import jakarta.persistence.*;                            // JPA 어노테이션들 임포트
import lombok.*;                                         // 롬복

import java.time.OffsetDateTime;                         // 시간 타입

@Entity
@Table(name = "users") // 테이블 이름 users로 고정.
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder // getter/setter, 생성자, 빌더 자동 생성을 묶어놓음
public class User {
    @Id // pk임
    @GeneratedValue(strategy = GenerationType.IDENTITY) // identity 사용해 insert할 때 DB가 알아서 숫자 채워줌.
    private Long id; // 유저 고유 번호.

    @Column(unique = true) // 같은 값 두 번 못 들어가게 함
    private String email; // 유저 이메일.

    private String nickname; // 유저 닉네임.

    @Column(nullable = false) // null 안되게 함.
    private OffsetDateTime createdAt; // 계정 만든 시각 저장하는 필드.

    @PrePersist // insert 되기 직전에 실행되는 메서드
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now(); // createdAt가 비어있으면 지금 시각 넣어줌.
    }
}
