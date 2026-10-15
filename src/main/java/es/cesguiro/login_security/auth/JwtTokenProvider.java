package es.cesguiro.login_security.auth;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import es.cesguiro.login_security.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_security.domain.model.User;
import es.cesguiro.login_security.domain.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

// Proveedor de JWT firmado con HS256 para el access token.
// En JWT puro el principal se construye solo con los claims del token,
// sin consultar la base de datos. Ver README para las limitaciones didácticas.
@Component
public class JwtTokenProvider implements JwtService {

	private final SecretKey secretKey;
	private final String issuer;

	public JwtTokenProvider(
			@Value("${app.jwt.secret}") String base64Secret,
			@Value("${app.jwt.issuer}") String issuer) {
		this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
		this.issuer = issuer;
	}

	@Override
	public String generateAccessToken(User user, Instant issuedAt, Instant expiresAt) {
		return Jwts.builder()
				.issuer(issuer)
				.subject(user.id().toString())
				.claim("roles", user.roles().stream().sorted().toList())
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();
	}

	@Override
	public Optional<AuthenticatedPrincipal> parseAccessToken(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(secretKey)
					.requireIssuer(issuer)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			Long userId = Long.valueOf(claims.getSubject());
			Set<String> roles = extractRoles(claims);
			return Optional.of(new AuthenticatedPrincipal(userId, roles));
		} catch (JwtException | IllegalArgumentException | NullPointerException exception) {
			return Optional.empty();
		}
	}

	private Set<String> extractRoles(Claims claims) {
		Object rolesClaim = claims.get("roles");
		if (!(rolesClaim instanceof Collection<?> roles)) {
			return Set.of();
		}
		Set<String> result = new HashSet<>();
		for (Object role : roles) {
			if (role != null) {
				result.add(role.toString());
			}
		}
		return Set.copyOf(result);
	}
}
