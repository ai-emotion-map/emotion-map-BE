package com.emomap.emomap.user.entity;                  // 유저 엔티티 패키지

import com.emomap.emomap.common.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;                            // JPA 어노테이션들 임포트
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;                                         // 롬복

import java.time.OffsetDateTime;                         // 시간 타입

@Entity
@Table(name = "Users") // 테이블 이름 users로 고정.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true) // getter/setter, 생성자, 빌더 자동 생성을 묶어놓음
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends BaseEntity {
    @Id // pk임
    @GeneratedValue(strategy = GenerationType.IDENTITY) // identity 사용해 insert할 때 DB가 알아서 숫자 채워줌.
    private Long id; // 유저 고유 번호.

    @Email
    @Column(unique = true, nullable = false) // 같은 값 두 번 못 들어가게 함
    private String email; // 유저 이메일.

    @NotBlank
    private String nickname; // 유저 닉네임.

    @NotBlank
    private String password;
}
