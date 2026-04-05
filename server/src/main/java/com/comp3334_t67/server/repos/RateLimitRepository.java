package com.comp3334_t67.server.repos;

import com.comp3334_t67.server.models.RateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RateLimitRepository extends JpaRepository<RateLimit, UUID> {

    RateLimit findByKey(String key);
    
}