package es.cesguiro.login_security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import es.cesguiro.login_security.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_security.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

class JwtTokenProviderTests {

	private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);
	private static final String ISSUER = "login-jwt";

	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, ISSUER);

	@Test
	void generatesSignedTokenAndParsesPrincipalFromClaims() {
		User user = new User(42L, "demo.user@example.com", "Usuario", "hash", true, Set.of("ROLE_USER"));
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plusSeconds(900);

		String token = jwtTokenProvider.generateAccessToken(user, issuedAt, expiresAt);

		assertEquals(3, token.split("\\.").length);
		assertEquals(
				Optional.of(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER"))),
				jwtTokenProvider.parseAccessToken(token));
	}

	@Test
	void rejectsTamperedAndExpiredTokens() {
		User user = new User(42L, "demo.user@example.com", "Usuario", "hash", true, Set.of("ROLE_USER"));
		Instant now = Instant.now();
		String validToken = jwtTokenProvider.generateAccessToken(user, now, now.plusSeconds(900));

		String tamperedToken = validToken.substring(0, validToken.length() - 1)
				+ (validToken.endsWith("a") ? "b" : "a");
		assertTrue(jwtTokenProvider.parseAccessToken(tamperedToken).isEmpty());

		String expiredToken = jwtTokenProvider.generateAccessToken(
				user, now.minusSeconds(7200), now.minusSeconds(3600));
		assertTrue(jwtTokenProvider.parseAccessToken(expiredToken).isEmpty());
	}

	@Test
	void rejectsTokensFromAnotherIssuer() {
		User user = new User(42L, "demo.user@example.com", "Usuario", "hash", true, Set.of("ROLE_USER"));
		Instant now = Instant.now();
		String foreignToken = Jwts.builder()
				.issuer("other-issuer")
				.subject("42")
				.claim("roles", java.util.List.of("ROLE_USER"))
				.issuedAt(java.util.Date.from(now))
				.expiration(java.util.Date.from(now.plusSeconds(900)))
				.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
				.compact();

		assertTrue(jwtTokenProvider.parseAccessToken(foreignToken).isEmpty());
		assertTrue(jwtTokenProvider.parseAccessToken(null).isEmpty());
		assertTrue(jwtTokenProvider.parseAccessToken("not-a-jwt").isEmpty());
	}
}
