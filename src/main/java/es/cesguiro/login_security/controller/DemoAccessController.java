package es.cesguiro.login_security.controller;

import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.cesguiro.login_security.domain.model.AuthenticatedPrincipal;

@RestController
@RequestMapping("/demo")
public class DemoAccessController {

	@GetMapping("/public")
	public DemoMessageResponse publicRoute() {
		return new DemoMessageResponse("Ruta pública.");
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/profile")
	public PrincipalResponse authenticatedRoute(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		return new PrincipalResponse(principal.userId(), principal.roles());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin")
	public DemoMessageResponse adminRoute() {
		return new DemoMessageResponse("Acceso de administrador.");
	}

	public record DemoMessageResponse(String message) {
	}

	public record PrincipalResponse(Long userId, Set<String> roles) {
	}
}
