package es.cesguiro.login_security.domain.exception;

public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UserNotFoundException(Long userId) {
		super("No se encontró el usuario con id " + userId + ".");
	}
}
