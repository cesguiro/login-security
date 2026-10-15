package es.cesguiro.login_security.domain.model;

public record TokenPair(
		String accessToken,
		String refreshToken) {
}
