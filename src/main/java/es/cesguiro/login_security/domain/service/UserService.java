package es.cesguiro.login_security.domain.service;

import es.cesguiro.login_security.domain.model.User;

public interface UserService {

	User findById(Long userId);
}
