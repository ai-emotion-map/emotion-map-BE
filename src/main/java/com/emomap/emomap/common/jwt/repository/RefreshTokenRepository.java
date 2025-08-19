package com.emomap.emomap.common.jwt.repository;

import com.emomap.emomap.common.jwt.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
