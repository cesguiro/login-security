package es.cesguiro.login_security.controller.mapper;

import org.springframework.stereotype.Component;

import es.cesguiro.login_security.controller.dto.UserResponse;
import es.cesguiro.login_security.domain.model.User;

@Component
public class UserResponseMapper {

	public UserResponse toUserResponse(User user) {
		return new UserResponse(user.id(), user.email(), user.displayName(), user.roles());
	}
}
