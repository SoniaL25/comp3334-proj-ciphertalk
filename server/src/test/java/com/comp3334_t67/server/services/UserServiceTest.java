package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.Exceptions.SelfBlockNotAllowedException;
import com.comp3334_t67.server.dtos.KeyDto;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.BlockedUserRepository;
import com.comp3334_t67.server.repos.UserRepository;

class UserServiceTest {

    private UserRepository userRepo;
    private BlockedUserRepository blockedRepo;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        blockedRepo = mock(BlockedUserRepository.class);
        service = new UserService(userRepo, blockedRepo);
    }

    @Test
    void getUserInfoById_shouldReturnDto() {
        UUID id = UUID.randomUUID();
        when(userRepo.findById(id)).thenReturn(Optional.of(User.builder().id(id).email("a@x.com").build()));

        var dto = service.getUserInfoById(id.toString());

        assertEquals("a@x.com", dto.getEmail());
    }

    @Test
    void getPublicKey_shouldReturnKeyDto() {
        UUID id = UUID.randomUUID();
        when(userRepo.findById(id)).thenReturn(Optional.of(User.builder().id(id).identityPublicKey("pk").build()));

        KeyDto dto = service.getPublicKey(id.toString());

        assertEquals("pk", dto.getPublicKey());
    }

    @Test
    void blockUser_shouldThrowOnSelfBlock() {
        UUID id = UUID.randomUUID();
        User u = User.builder().id(id).email("a@x.com").build();
        when(userRepo.findByEmail("a@x.com")).thenReturn(u);

        assertThrows(SelfBlockNotAllowedException.class,
            () -> service.blockUser("a@x.com", "a@x.com"));
    }

    @Test
    void blockUser_shouldSaveWhenNotExisting() {
        UUID blocker = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        when(userRepo.findByEmail("b1@x.com")).thenReturn(User.builder().id(blocker).email("b1@x.com").build());
        when(userRepo.findByEmail("b2@x.com")).thenReturn(User.builder().id(blocked).email("b2@x.com").build());
        when(blockedRepo.existsByUserIdAndBlockedUserId(blocker, blocked)).thenReturn(false);

        service.blockUser("b1@x.com", "b2@x.com");

        verify(blockedRepo).save(any());
    }

    @Test
    void unblockUser_shouldDeleteRelationship() {
        UUID blocker = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        when(userRepo.findByEmail("b1@x.com")).thenReturn(User.builder().id(blocker).email("b1@x.com").build());
        when(userRepo.findByEmail("b2@x.com")).thenReturn(User.builder().id(blocked).email("b2@x.com").build());

        service.unblockUser("b1@x.com", "b2@x.com");

        verify(blockedRepo).deleteByUserIdAndBlockedUserId(blocker, blocked);
    }
}
