package br.unifor.healthsys.user.security;

import java.io.Serializable;
import java.time.Instant;

public record CachedTokenDetails(
        String username,
        String role,
        String email,
        String nome,
        Long userId,
        Instant expiresAt
) implements Serializable {
}
