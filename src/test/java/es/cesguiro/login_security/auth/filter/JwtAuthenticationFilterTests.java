package es.cesguiro.login_security.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import es.cesguiro.login_security.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_security.domain.model.TokenPair;
import es.cesguiro.login_security.domain.service.AuthService;
import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTests {

	private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(new StubAuthService());

	@BeforeEach
	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void extractsTokenFromBearerAuthorizationHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer jwt-token-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		jwtAuthenticationFilter.doFilter(request, response, emptyFilterChain());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertEquals(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER")), authentication.getPrincipal());
		assertTrue(authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
	}

	@Test
	void extractsTokenWhenBearerSchemeHasDifferentCase() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "bEaReR jwt-token-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		jwtAuthenticationFilter.doFilter(request, response, emptyFilterChain());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertEquals(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER")), authentication.getPrincipal());
	}

	@Test
	void leavesSecurityContextEmptyWhenAuthorizationHeaderIsMissing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		jwtAuthenticationFilter.doFilter(request, response, emptyFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void leavesSecurityContextEmptyWhenAuthorizationHeaderIsMalformed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic jwt-token-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		jwtAuthenticationFilter.doFilter(request, response, emptyFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void leavesSecurityContextEmptyWhenBearerTokenIsEmptyOrContainsWhitespace() throws Exception {
		MockHttpServletRequest emptyTokenRequest = new MockHttpServletRequest();
		emptyTokenRequest.addHeader("Authorization", "Bearer   ");
		MockHttpServletRequest whitespaceTokenRequest = new MockHttpServletRequest();
		whitespaceTokenRequest.addHeader("Authorization", "Bearer token with-spaces");
		MockHttpServletResponse response = new MockHttpServletResponse();

		jwtAuthenticationFilter.doFilter(emptyTokenRequest, response, emptyFilterChain());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		jwtAuthenticationFilter.doFilter(whitespaceTokenRequest, response, emptyFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void leavesSecurityContextEmptyWhenTokenIsUnknown() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer unknown-token");
		MockHttpServletResponse response = new MockHttpServletResponse();

		jwtAuthenticationFilter.doFilter(request, response, emptyFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	private static FilterChain emptyFilterChain() {
		return (filteredRequest, filteredResponse) -> {
		};
	}

	private static class StubAuthService implements AuthService {

		@Override
		public TokenPair login(String email, String password) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<AuthenticatedPrincipal> authenticate(String accessToken) {
			return "jwt-token-123".equals(accessToken)
					? Optional.of(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER")))
					: Optional.empty();
		}
	}
}
