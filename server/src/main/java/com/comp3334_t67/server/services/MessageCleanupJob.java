package com.comp3334_t67.server.services;

import com.comp3334_t67.server.repos.MessageRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class MessageCleanupJob {

    private final MessageRepository messageRepository;

    // Run every 5 minutes (cron expression: second, minute, hour, day of month, month, day of week)
    @Scheduled(cron = "0 */5 * * * *")
    public void deleteExpiredMessages() {
        messageRepository.deleteExpiredMessages(LocalDateTime.now());
    }
}
