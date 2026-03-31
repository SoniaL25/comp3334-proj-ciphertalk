package com.comp3334_t67.server.repos;

import com.comp3334_t67.server.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import com.comp3334_t67.server.enums.MessageStatus;

import java.util.*;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByReceiverIdAndChatIdAndStatus(UUID receiverId, UUID chatId, MessageStatus status);
}
