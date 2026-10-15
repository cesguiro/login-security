package es.cesguiro.login_security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationsTests {

	@Container
	static final MariaDBContainer database = new MariaDBContainer(DockerImageName.parse("mariadb:12.3"));

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void configureDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", database::getJdbcUrl);
		registry.add("spring.datasource.username", database::getUsername);
		registry.add("spring.datasource.password", database::getPassword);
	}

	@Test
	void migrationsCreateAllTablesAndSeedDemoUsersAndRoles() {
		assertEquals(4, jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN ('users', 'roles', 'user_roles', 'refresh_tokens')
				""", Integer.class));
		assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
		assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class));
		assertEquals(3, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_roles", Integer.class));
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refresh_tokens", Integer.class));
	}

	@Test
	void migrationsPreventDuplicateEmailsAndUserRoleAssignments() {
		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				INSERT INTO users (email, display_name, password_hash)
				VALUES ('demo.user@example.com', 'Duplicado', 'not-a-real-hash')
				"""));

		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				INSERT INTO user_roles (user_id, role_id)
				SELECT users.id, roles.id
				FROM users CROSS JOIN roles
				WHERE users.email = 'demo.user@example.com' AND roles.name = 'ROLE_USER'
				"""));
	}

	@Test
	void tokenTablesRejectExpirationBeforeIssueTime() {
		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				INSERT INTO refresh_tokens (user_id, token_hash, issued_at, expires_at)
				SELECT id, UNHEX(REPEAT('00', 32)), CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6) - INTERVAL 1 SECOND
				FROM users WHERE email = 'demo.user@example.com'
				"""));
	}
}
