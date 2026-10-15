package es.cesguiro.login_security.domain.repository;

import java.util.Optional;

import es.cesguiro.login_security.domain.model.User;

public interface UserRepository {

	Optional<User> findByEmail(String email);

	Optional<User> findById(Long id);
}
