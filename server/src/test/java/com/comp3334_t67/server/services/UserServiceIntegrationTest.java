package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.TestKeyFactory;
import com.comp3334_t67.server.Exceptions.InvalidInputException;
import com.comp3334_t67.server.Exceptions.SelfBlockNotAllowedException;
import com.comp3334_t67.server.Exceptions.UserNotFoundException;
import com.comp3334_t67.server.dtos.KeyDto;
import com.comp3334_t67.server.dtos.UserDto;
import com.comp3334_t67.server.models.User;

@SpringBootTest
class UserServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserService userService;

    @Test
    void getUserInfoByEmail_shouldReturnUserDto() {
        // Arrange: persist one user.
        User user = userRepo.save(User.builder().email("USER1@EXAMPLE.COM").password("x".getBytes()).build());

        // Act: fetch profile by email.
        UserDto dto = userService.getUserInfoByEmail(user.getEmail());

        // Assert: dto contains expected email.
        assertEquals(user.getEmail(), dto.getEmail());
    }

    @Test
    void getUserInfoById_shouldThrow_whenUserMissing() {
        // Arrange: prepare random id that does not exist.
        String missingId = UUID.randomUUID().toString();

        // Act + Assert: missing id throws not-found.
        assertThrows(UserNotFoundException.class, () -> userService.getUserInfoById(missingId));
    }

    @Test
    void deleteUserById_shouldRemoveUser() {
        // Arrange: seed one user.
        User user = userRepo.save(User.builder().email("DEL@EXAMPLE.COM").password("x".getBytes()).build());

        // Act: delete user by id.
        assertDoesNotThrow(() -> userService.deleteUserById(user.getId().toString()));

        // Assert: repository no longer contains the user.
        assertTrue(userRepo.findById(user.getId()).isEmpty());
    }

    @Test
    void uploadPublicKey_shouldValidateAndPersistKey() throws Exception {
        // Arrange: create user and valid rsa pem key.
        User user = userRepo.save(User.builder().email("KEY@EXAMPLE.COM").password("x".getBytes()).build());
        String pem = TestKeyFactory.generatePemPublicKey();

        // Act: upload key.
        assertDoesNotThrow(() -> userService.uploadPublicKey(user.getEmail(), pem));

        // Assert: key and timestamp are persisted.
        User saved = userRepo.findById(user.getId()).orElseThrow();
        assertEquals(pem, saved.getIdentityPublicKey());
        assertNotNull(saved.getKeyUpdatedAt());
    }

    @Test
    void uploadPublicKey_shouldThrow_whenKeyIsInvalid() {
        // Arrange: create user and bad key.
        User user = userRepo.save(User.builder().email("BADKEY@EXAMPLE.COM").password("x".getBytes()).build());

        // Act + Assert: invalid key is rejected.
        assertThrows(InvalidInputException.class, () -> userService.uploadPublicKey(user.getEmail(), "bad-key"));
    }

    @Test
    void getPublicKeyByEmailAndId_shouldReturnSamePayload() {
        // Arrange: persist user with key metadata.
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        User user = userRepo.save(User.builder().email("PK@EXAMPLE.COM").password("x".getBytes()).identityPublicKey("PEM").keyUpdatedAt(now).build());

        // Act: fetch key by both lookup methods.
        KeyDto byEmail = userService.getPublicKeyByEmail(user.getEmail());
        KeyDto byId = userService.getPublicKeyById(user.getId().toString());

        // Assert: both outputs are consistent.
        assertEquals("PEM", byEmail.getPublicKey());
        assertEquals(now, byEmail.getUploadedAt());
        assertEquals("PEM", byId.getPublicKey());
        assertEquals(now, byId.getUploadedAt());
    }

    @Test
    void blockUser_shouldPersistBlock_andUnblockShouldDelete() {
        // Arrange: create blocker and target users.
        User blocker = userRepo.save(User.builder().email("BLOCKER2@EXAMPLE.COM").password("x".getBytes()).build());
        User target = userRepo.save(User.builder().email("TARGET2@EXAMPLE.COM").password("x".getBytes()).build());

        // Act: block and then unblock.
        userService.blockUser(blocker.getEmail(), target.getId().toString());
        userService.unblockUser(blocker.getEmail(), target.getId().toString());

        // Assert: no blocked relation remains.
        assertTrue(blockedUserRepo.findByUserIdAndBlockedUserId(blocker.getId(), target.getId()).isEmpty());
    }

    @Test
    void blockUser_shouldThrow_whenBlockingSelf() {
        // Arrange: persist single user.
        User user = userRepo.save(User.builder().email("SELF@EXAMPLE.COM").password("x".getBytes()).build());

        // Act + Assert: self-block is forbidden.
        assertThrows(SelfBlockNotAllowedException.class, () -> userService.blockUser(user.getEmail(), user.getId().toString()));
    }
}
