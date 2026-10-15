package es.cesguiro.login_security.controller.mapper;

import org.springframework.stereotype.Component;

import es.cesguiro.login_security.controller.dto.LoginResponse;
import es.cesguiro.login_security.domain.model.TokenPair;

@Component
public class AuthMapper {

	public LoginResponse toLoginResponse(TokenPair tokenPair) {
		return new LoginResponse(
				tokenPair.accessToken(),
				"Bearer",
				tokenPair.refreshToken());
	}
}
