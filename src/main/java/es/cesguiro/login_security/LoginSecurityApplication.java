package es.cesguiro.login_security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Se excluye el usuario en memoria de Boot: la autenticación es stateless con JWT
// y los usuarios viven en MariaDB, no hace falta ningún UserDetailsService.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LoginSecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoginSecurityApplication.class, args);
	}

}
