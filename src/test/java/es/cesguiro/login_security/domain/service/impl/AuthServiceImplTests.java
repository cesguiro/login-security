package es.cesguiro.login_security.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import es.cesguiro.login_security.domain.exception.InvalidCredentialsException;
import es.cesguiro.login_security.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_security.domain.model.StoredToken;
import es.cesguiro.login_security.domain.model.TokenPair;
import es.cesguiro.login_security.domain.model.User;
import es.cesguiro.login_security.domain.repository.TokenRepository;
import es.cesguiro.login_security.domain.repository.UserRepository;
import es.cesguiro.login_security.domain.service.JwtService;

class AuthServiceImplTests {

	private static final String PASSWORD_HASH = "$2b$10$cP0LTHNNH2vByI4MwnJvUOS5cmDjfnUSH8xHpf8EDwOPGaBPhk4.2";
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

	@Test
	void loginReturnsJwtAccessTokenAndPersistsOnlyRefreshHash() {
		AtomicReference<StoredToken> savedRefreshToken = new AtomicReference<>();
		AuthServiceImpl authService = createAuthService(
				email -> Optional.of(enabledUser()),
				savedRefreshToken::set,
				token -> Optional.of(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER"))));

		Instant testStartedAt = Instant.now();
		TokenPair tokenPair = authService.login("demo.user@example.com", "password");
		Instant testCompletedAt = Instant.now();
		Instant issuedAt = savedRefreshToken.get().issuedAt();

		// El access token es un JWT firmado generado por JwtService.
		assertEquals("jwt-for-42", tokenPair.accessToken());
		// El refresh token sigue siendo opaco y solo se guarda su hash.
		assertTrue(tokenPair.refreshToken().matches("[A-Za-z0-9_-]+"));
		assertFalse(issuedAt.isBefore(testStartedAt));
		assertFalse(issuedAt.isAfter(testCompletedAt));
		assertEquals(42L, savedRefreshToken.get().userId());
		assertArrayEquals(sha256(tokenPair.refreshToken()), savedRefreshToken.get().tokenHash());
		assertEquals(issuedAt.plus(REFRESH_TOKEN_TTL), savedRefreshToken.get().expiresAt());
	}

	@Test
	void loginRejectsUnknownUserWithoutCreatingTokens() {
		AtomicReference<StoredToken> savedRefreshToken = new AtomicReference<>();
		AuthServiceImpl authService = createAuthService(
				email -> Optional.empty(),
				savedRefreshToken::set,
				token -> Optional.empty());

		assertThrows(InvalidCredentialsException.class, () -> authService.login("unknown@example.com", "password"));

		assertNull(savedRefreshToken.get());
	}

	@Test
	void loginRejectsIncorrectPasswordWithoutCreatingTokens() {
		AtomicReference<StoredToken> savedRefreshToken = new AtomicReference<>();
		AuthServiceImpl authService = createAuthService(
				email -> Optional.of(enabledUser()),
				savedRefreshToken::set,
				token -> Optional.empty());

		assertThrows(InvalidCredentialsException.class, () -> authService.login("demo.user@example.com", "wrong"));

		assertNull(savedRefreshToken.get());
	}

	@Test
	void loginRejectsDisabledUserWithoutCreatingTokens() {
		AtomicReference<StoredToken> savedRefreshToken = new AtomicReference<>();
		AuthServiceImpl authService = createAuthService(
				email -> Optional.of(new User(42L, "demo.user@example.com", "Usuario", PASSWORD_HASH, false, Set.of("ROLE_USER"))),
				savedRefreshToken::set,
				token -> Optional.empty());

		assertThrows(InvalidCredentialsException.class, () -> authService.login("demo.user@example.com", "password"));

		assertNull(savedRefreshToken.get());
	}

	@Test
	void authenticateReturnsPrincipalFromJwtClaimsWithoutDatabaseLookup() {
		AuthServiceImpl authService = createAuthService(
				email -> {
					throw new AssertionError("No se debe consultar la base de datos en JWT puro.");
				},
				token -> {
					throw new AssertionError("No se debe guardar nada al autenticar.");
				},
				token -> Optional.of(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER"))));

		Optional<AuthenticatedPrincipal> principal = authService.authenticate("valid-jwt");

		assertEquals(Optional.of(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER"))), principal);
	}

	@Test
	void authenticateReturnsEmptyWhenJwtIsInvalid() {
		AuthServiceImpl authService = createAuthService(
				email -> Optional.of(enabledUser()),
				token -> {
				},
				token -> Optional.empty());

		assertTrue(authService.authenticate("tampered-jwt").isEmpty());
		assertTrue(authService.authenticate(null).isEmpty());
		assertTrue(authService.authenticate("  ").isEmpty());
	}

	@Test
	void authenticateStillAcceptsTokenWhenUserIsDisabledInDatabase() {
		// Limitación didáctica del JWT puro: al no consultar la base de datos,
		// un token emitido sigue válido hasta que caduca aunque el usuario se deshabilite.
		AuthServiceImpl authService = createAuthService(
				email -> Optional.of(new User(42L, "demo.user@example.com", "Usuario", PASSWORD_HASH, false, Set.of("ROLE_USER"))),
				token -> {
				},
				token -> Optional.of(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER"))));

		assertEquals(
				Optional.of(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER"))),
				authService.authenticate("valid-jwt"));
	}

	private AuthServiceImpl createAuthService(
			Function<String, Optional<User>> userLookup,
			Consumer<StoredToken> tokenSaver,
			Function<String, Optional<AuthenticatedPrincipal>> principalLookup) {
		UserRepository userRepository = new UserRepository() {
			@Override
			public Optional<User> findByEmail(String email) {
				return userLookup.apply(email);
			}

			@Override
			public Optional<User> findById(Long id) {
				throw new UnsupportedOperationException("JWT puro no consulta usuarios al autenticar.");
			}
		};
		TokenRepository tokenRepository = tokenSaver::accept;
		JwtService jwtService = new JwtService() {
			@Override
			public String generateAccessToken(User user, Instant issuedAt, Instant expiresAt) {
				return "jwt-for-" + user.id();
			}

			@Override
			public Optional<AuthenticatedPrincipal> parseAccessToken(String token) {
				return principalLookup.apply(token);
			}
		};
		return new AuthServiceImpl(
				userRepository,
				tokenRepository,
				jwtService,
				new BCryptPasswordEncoder(),
				ACCESS_TOKEN_TTL,
				REFRESH_TOKEN_TTL);
	}

	private User enabledUser() {
		return new User(42L, "demo.user@example.com", "Usuario", PASSWORD_HASH, true, Set.of("ROLE_USER"));
	}

	private byte[] sha256(String token) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
