package es.cesguiro.login_security.domain.service.impl;

import org.springframework.stereotype.Service;

import es.cesguiro.login_security.domain.exception.UserNotFoundException;
import es.cesguiro.login_security.domain.model.User;
import es.cesguiro.login_security.domain.repository.UserRepository;
import es.cesguiro.login_security.domain.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;

	public UserServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public User findById(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
	}
}
