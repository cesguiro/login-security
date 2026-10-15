package es.cesguiro.login_security.domain.service;

import java.time.Instant;
import java.util.Optional;

import es.cesguiro.login_security.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_security.domain.model.User;

// Puerto del dominio para el access token JWT.
// La implementación vive en auth y usa una librería externa; el dominio solo ve esta abstracción.
// En JWT puro el principal se construye solo con los claims del token, sin consultar la base de datos.
public interface JwtService {

	String generateAccessToken(User user, Instant issuedAt, Instant expiresAt);

	Optional<AuthenticatedPrincipal> parseAccessToken(String token);
}
