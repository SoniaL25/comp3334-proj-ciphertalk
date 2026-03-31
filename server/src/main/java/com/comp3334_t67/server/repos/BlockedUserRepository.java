package com.comp3334_t67.server.repos;

import com.comp3334_t67.server.models.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, UUID> {
    Optional<BlockedUser> findByUserIdAndBlockedUserId(UUID userId, UUID blockedUserId);

    boolean existsByUserIdAndBlockedUserId(UUID userId, UUID blockedUserId);

    void deleteByUserIdAndBlockedUserId(UUID userId, UUID blockedUserId);

    List<BlockedUser> findByUserId(UUID userId);
}