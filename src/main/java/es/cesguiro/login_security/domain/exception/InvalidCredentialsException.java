package es.cesguiro.login_security.domain.exception;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Credenciales no válidas.");
	}
}
