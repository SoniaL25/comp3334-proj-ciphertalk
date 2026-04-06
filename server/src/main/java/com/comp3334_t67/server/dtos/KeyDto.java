package com.comp3334_t67.server.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyDto {
    private String publicKey;
    private LocalDateTime uploadedAt;
}
