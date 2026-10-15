package es.cesguiro.login_security.auth.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import es.cesguiro.login_security.domain.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filtro de autenticación JWT integrado con Spring Security.
// Extrae el Bearer de la cabecera Authorization, lo valida con el dominio
// y publica la autenticación en el SecurityContextHolder.
// La autorización por roles se hace después con @PreAuthorize.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_SCHEME = "Bearer";
	private final AuthService authService;

	public JwtAuthenticationFilter(AuthService authService) {
		this.authService = authService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String accessToken = extractAccessToken(request.getHeader("Authorization"));
		if (accessToken != null) {
			authService.authenticate(accessToken).ifPresent(principal -> {
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						principal,
						null,
						principal.roles().stream().map(SimpleGrantedAuthority::new).toList());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			});
		}
		filterChain.doFilter(request, response);
	}

	private String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null) {
			return null;
		}

		int schemeEnd = authorizationHeader.indexOf(' ');
		if (schemeEnd < 0 || !BEARER_SCHEME.equalsIgnoreCase(authorizationHeader.substring(0, schemeEnd))) {
			return null;
		}

		String accessToken = authorizationHeader.substring(schemeEnd).stripLeading();
		if (accessToken.isEmpty() || accessToken.chars().anyMatch(Character::isWhitespace)) {
			return null;
		}

		return accessToken;
	}
}
