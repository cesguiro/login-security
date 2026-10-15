package es.cesguiro.login_security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.cesguiro.login_security.controller.dto.LoginRequest;
import es.cesguiro.login_security.controller.dto.LoginResponse;
import es.cesguiro.login_security.controller.dto.UserResponse;
import es.cesguiro.login_security.controller.mapper.AuthMapper;
import es.cesguiro.login_security.controller.mapper.UserResponseMapper;
import es.cesguiro.login_security.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_security.domain.service.AuthService;
import es.cesguiro.login_security.domain.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthMapper authMapper;
	private final UserService userService;
	private final UserResponseMapper userMapper;

	public AuthController(
			AuthService authService,
			AuthMapper authMapper,
			UserService userService,
			UserResponseMapper userMapper) {
		this.authService = authService;
		this.authMapper = authMapper;
		this.userService = userService;
		this.userMapper = userMapper;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
		return ResponseEntity.ok(authMapper.toLoginResponse(
				authService.login(loginRequest.email(), loginRequest.password())));
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/me")
	public UserResponse currentUser(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		return userMapper.toUserResponse(userService.findById(principal.userId()));
	}
}
