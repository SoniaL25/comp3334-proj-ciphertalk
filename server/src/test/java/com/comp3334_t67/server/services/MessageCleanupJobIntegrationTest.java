package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.enums.MessageStatus;
import com.comp3334_t67.server.models.Message;

@SpringBootTest
class MessageCleanupJobIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MessageCleanupJob job;

    @Test
    void deleteExpiredMessages_shouldDeleteOnlyExpiredRows() {
        // Arrange: insert one expired and one active message.
        messageRepo.save(Message.builder().chatId(UUID.randomUUID()).senderId(UUID.randomUUID()).receiverId(UUID.randomUUID()).clientMessageId("c1").content("old").nonce("n1").status(MessageStatus.SENT).createdAt(LocalDateTime.now().minusMinutes(20)).expiresAt(LocalDateTime.now().minusMinutes(1)).build());
        messageRepo.save(Message.builder().chatId(UUID.randomUUID()).senderId(UUID.randomUUID()).receiverId(UUID.randomUUID()).clientMessageId("c2").content("new").nonce("n2").status(MessageStatus.SENT).createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(10)).build());

        // Act: run cleanup.
        job.deleteExpiredMessages();

        // Assert: only the active message remains.
        assertEquals(1, messageRepo.findAll().size());
        assertEquals("c2", messageRepo.findAll().get(0).getClientMessageId());
    }
}
