package es.cesguiro.login_security.domain.model;

import java.time.Instant;

public record StoredToken(Long userId, byte[] tokenHash, Instant issuedAt, Instant expiresAt) {
}
