package com.comp3334_t67.server.repos;

import com.comp3334_t67.server.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.comp3334_t67.server.enums.MessageStatus;

import java.time.LocalDateTime;
import java.util.*;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByReceiverIdAndChatIdAndStatus(UUID receiverId, UUID chatId, MessageStatus status);

    Optional<Message> findTopByChatIdOrderByCreatedAtDesc(UUID chatId);

    long countByReceiverIdAndChatIdAndStatus(UUID receiverId, UUID chatId, MessageStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.expiresAt IS NOT NULL AND m.expiresAt < :now")
    int deleteExpiredMessages(@Param("now") LocalDateTime now);

    // Check if message with the same (clientMessageId, senderId, chatId) already exists
    boolean existsByClientMessageIdAndSenderIdAndChatId(String clientMessageId, UUID senderId, UUID chatId);
}
