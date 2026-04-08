package com.comp3334_t67.server.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;
    @Column(name = "password")
    private byte[] password;

    @Column(name = "identity_public_key", columnDefinition = "TEXT")
    private String identityPublicKey;

    @Column(name = "key_updated_at")
    private LocalDateTime keyUpdatedAt;

    @Builder.Default
    @Column(name = "otp_secret", nullable = false)
    private int otpSecret = 0;

    @Builder.Default
    @Column(name = "otp_expiry", nullable = false)
    private LocalDateTime otpExpiry = LocalDateTime.now();
    
}
