package com.zidtech.auth.repository;

import com.zidtech.auth.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByHashedValue(String hashedValue);
    List<RefreshToken> findAllByFamilyId(UUID familyId);
}