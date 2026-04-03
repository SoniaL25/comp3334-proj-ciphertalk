package com.comp3334_t67.server.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.repos.MessageRepository;

class MessageCleanupJobTest {

    @Test
    void deleteExpiredMessages_shouldCallRepository() {
        MessageRepository repo = mock(MessageRepository.class);
        MessageCleanupJob job = new MessageCleanupJob(repo);

        job.deleteExpiredMessages();

        verify(repo).deleteExpiredMessages(any());
    }
}
