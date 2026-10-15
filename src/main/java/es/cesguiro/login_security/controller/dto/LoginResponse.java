package es.cesguiro.login_security.controller.dto;

public record LoginResponse(
		String accessToken,
		String tokenType,
		String refreshToken) {
}
