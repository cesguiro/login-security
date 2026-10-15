package es.cesguiro.login_security.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Traduce los fallos de autorización al formato de error de la API.
// Se usa cuando el usuario está autenticado pero no tiene el rol necesario.
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException exception) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		// Mensaje fijo sin datos de usuario: no hay nada que escapar.
		response.getWriter().write("{\"message\":\"No tienes permisos para realizar esta operación.\"}");
	}
}
