package com.comp3334_t67.server.repos;

import com.comp3334_t67.server.models.FriendChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface FriendChatRepository extends JpaRepository<FriendChat, UUID> {

    @Query("SELECT c FROM FriendChat c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    List<FriendChat> findAllByUserId(@Param("userId") UUID userId);
    
    FriendChat findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);
}
