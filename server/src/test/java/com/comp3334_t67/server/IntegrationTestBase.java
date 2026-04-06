package com.comp3334_t67.server;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import com.comp3334_t67.server.repos.BlockedUserRepository;
import com.comp3334_t67.server.repos.FriendChatRepository;
import com.comp3334_t67.server.repos.FriendRequestRepository;
import com.comp3334_t67.server.repos.MessageRepository;
import com.comp3334_t67.server.repos.RateLimitRepository;
import com.comp3334_t67.server.repos.UserRepository;

@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected MessageRepository messageRepo;

    @Autowired
    protected BlockedUserRepository blockedUserRepo;

    @Autowired
    protected FriendRequestRepository friendRequestRepo;

    @Autowired
    protected FriendChatRepository friendChatRepo;

    @Autowired
    protected RateLimitRepository rateLimitRepo;

    @Autowired
    protected UserRepository userRepo;

    @AfterEach
    void cleanupDatabase() {
        // Delete children first to avoid FK violations.
        messageRepo.deleteAll();
        blockedUserRepo.deleteAll();
        friendRequestRepo.deleteAll();
        friendChatRepo.deleteAll();
        rateLimitRepo.deleteAll();
        userRepo.deleteAll();
    }
}
