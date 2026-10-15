package es.cesguiro.login_security.persistence.dao;

import java.util.Optional;

import es.cesguiro.login_security.domain.model.User;

public interface UserDao {

	Optional<User> findByEmail(String email);

	Optional<User> findById(Long id);
}
