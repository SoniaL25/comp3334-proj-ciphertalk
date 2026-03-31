package com.comp3334_t67.server.repos;

import com.comp3334_t67.server.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
}
