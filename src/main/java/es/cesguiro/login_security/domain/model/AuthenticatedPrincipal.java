package es.cesguiro.login_security.domain.model;

import java.security.Principal;
import java.util.Set;

public record AuthenticatedPrincipal(Long userId, Set<String> roles) implements Principal {

	public AuthenticatedPrincipal {
		roles = Set.copyOf(roles);
	}

	@Override
	public String getName() {
		return userId.toString();
	}
}
