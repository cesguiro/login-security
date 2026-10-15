package es.cesguiro.login_security.domain.model;

import java.util.Set;

public record User(
		Long id,
		String email,
		String displayName,
		String passwordHash,
		boolean enabled,
		Set<String> roles) {

	public User {
		roles = Set.copyOf(roles);
	}

	public User withRoles(Set<String> roles) {
		return new User(id, email, displayName, passwordHash, enabled, roles);
	}
}
