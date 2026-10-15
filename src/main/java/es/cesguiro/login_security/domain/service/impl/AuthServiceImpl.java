package es.cesguiro.login_security.domain.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.cesguiro.login_security.domain.exception.InvalidCredentialsException;
import es.cesguiro.login_security.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_security.domain.model.StoredToken;
import es.cesguiro.login_security.domain.model.TokenPair;
import es.cesguiro.login_security.domain.model.User;
import es.cesguiro.login_security.domain.repository.TokenRepository;
import es.cesguiro.login_security.domain.repository.UserRepository;
import es.cesguiro.login_security.domain.service.AuthService;
import es.cesguiro.login_security.domain.service.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final TokenRepository tokenRepository;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;

	public AuthServiceImpl(
			UserRepository userRepository,
			TokenRepository tokenRepository,
			JwtService jwtService,
			PasswordEncoder passwordEncoder,
			@Value("${app.auth.access-token-ttl}") Duration accessTokenTtl,
			@Value("${app.auth.refresh-token-ttl}") Duration refreshTokenTtl) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.jwtService = jwtService;
		this.passwordEncoder = passwordEncoder;
		this.accessTokenTtl = accessTokenTtl;
		this.refreshTokenTtl = refreshTokenTtl;
	}

	@Override
	@Transactional
	public TokenPair login(String email, String password) {
		if (email == null || email.isBlank() || password == null || password.isEmpty()) {
			throw new InvalidCredentialsException();
		}

		User user = userRepository.findByEmail(email.strip())
				.filter(User::enabled)
				.filter(foundUser -> passwordMatches(password, foundUser.passwordHash()))
				.orElseThrow(InvalidCredentialsException::new);

		Instant issuedAt = Instant.now();
		String accessToken = jwtService.generateAccessToken(user, issuedAt, issuedAt.plus(accessTokenTtl));
		String refreshToken = generateToken();

		tokenRepository.saveRefreshToken(
				new StoredToken(user.id(), hashToken(refreshToken), issuedAt, issuedAt.plus(refreshTokenTtl)));

		return new TokenPair(accessToken, refreshToken);
	}

	@Override
	public Optional<AuthenticatedPrincipal> authenticate(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			return Optional.empty();
		}

		// JWT puro: la identidad y los roles vienen solo de los claims del token.
		// No se consulta la base de datos, por eso un token sigue válido hasta que caduca
		// aunque el usuario se deshabilite o cambie de roles. Ver README.
		return jwtService.parseAccessToken(accessToken);
	}

	private boolean passwordMatches(String password, String passwordHash) {
		try {
			return passwordEncoder.matches(password, passwordHash);
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private String generateToken() {
		byte[] tokenBytes = new byte[32];
		secureRandom.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	private byte[] hashToken(String token) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("No se pudo calcular el hash del token.", exception);
		}
	}
}
