package es.cesguiro.login_security.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import es.cesguiro.login_security.domain.model.User;
import es.cesguiro.login_security.domain.repository.UserRepository;
import es.cesguiro.login_security.domain.service.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthLoginIntegrationTests {

	private static final String USER_EMAIL = "demo.user@example.com";
	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
	private static final Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("\"refreshToken\":\"([^\"]+)\"");

	@Container
	static final MariaDBContainer database = new MariaDBContainer(DockerImageName.parse("mariadb:12.3"));

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@DynamicPropertySource
	static void configureDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", database::getJdbcUrl);
		registry.add("spring.datasource.username", database::getUsername);
		registry.add("spring.datasource.password", database::getPassword);
	}

	@BeforeEach
	void clearIssuedTokensAndTestTriggers() {
		jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_refresh_token_insert");
		jdbcTemplate.update("DELETE FROM refresh_tokens");
	}

	@Test
	void loginReturnsJwtAccessTokenAndPersistsOnlyRefreshHash() throws Exception {
		assertEquals(Set.of("ROLE_USER"), userRepository.findByEmail(USER_EMAIL).orElseThrow().roles());

		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType("application/json")
				.content("""
						{"email":"demo.user@example.com","password":"password"}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessTokenExpiresAt").doesNotExist())
			.andExpect(jsonPath("$.refreshTokenExpiresAt").doesNotExist())
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		String accessToken = extractToken(ACCESS_TOKEN_PATTERN, responseBody);
		String refreshToken = extractToken(REFRESH_TOKEN_PATTERN, responseBody);
		assertFalse(accessToken.isBlank());
		assertFalse(refreshToken.isBlank());
		assertNotEquals(accessToken, refreshToken);
		// El access token JWT es autocontenido: tiene 3 segmentos y no se guarda en la base de datos.
		assertEquals(3, accessToken.split("\\.").length);
		assertEquals(1, countRows("refresh_tokens"));
		assertEquals(hashAsHex(refreshToken), storedHash("refresh_tokens"));
	}

	@Test
	void loginRejectsInvalidCredentialsWithGenericResponseAndDoesNotIssueTokens() throws Exception {
		mockMvc.perform(post("/auth/login")
				.contentType("application/json")
				.content("""
						{"email":"demo.user@example.com","password":"incorrect"}
						"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Credenciales no válidas."));

		assertEquals(0, countRows("refresh_tokens"));
	}

	@Test
	void loginRejectsInvalidRequestBody() throws Exception {
		mockMvc.perform(post("/auth/login")
				.contentType("application/json")
				.content("""
						{"email":"not-an-email","password":" "}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("La solicitud no es válida."));

		assertEquals(0, countRows("refresh_tokens"));
	}

	@Test
	void loginDoesNotPersistRefreshTokenWhenInsertFails() throws Exception {
		jdbcTemplate.execute("""
				CREATE TRIGGER fail_refresh_token_insert
				BEFORE INSERT ON refresh_tokens
				FOR EACH ROW
				SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fallo simulado'
				""");

		assertThrows(Exception.class, () -> mockMvc.perform(post("/auth/login")
				.contentType("application/json")
				.content("""
						{"email":"demo.user@example.com","password":"password"}
						""")));

		assertEquals(0, countRows("refresh_tokens"));
	}

	@Test
	void currentUserReturnsUserLoadedFromTheBearerToken() throws Exception {
		String accessToken = loginAndGetAccessToken(USER_EMAIL);

		mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userRepository.findByEmail(USER_EMAIL).orElseThrow().id()))
				.andExpect(jsonPath("$.email").value(USER_EMAIL))
				.andExpect(jsonPath("$.displayName").value("Usuario de ejemplo"))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void currentUserRejectsMissingOrInvalidAccessToken() throws Exception {
		mockMvc.perform(get("/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Se requiere autenticación."));

		mockMvc.perform(get("/auth/me").header("Authorization", "Bearer unknown-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Se requiere autenticación."));
	}

	@Test
	void demoPublicRouteDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/demo/public"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Ruta pública."));
	}

	@Test
	void demoAuthenticatedRouteRequiresAnAccessTokenAndReturnsPrincipal() throws Exception {
		mockMvc.perform(get("/demo/profile"))
				.andExpect(status().isUnauthorized());

		String accessToken = loginAndGetAccessToken(USER_EMAIL);
		mockMvc.perform(get("/demo/profile").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(userRepository.findByEmail(USER_EMAIL).orElseThrow().id()))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
	}

	@Test
	void demoRoleRouteRequiresTheConfiguredRole() throws Exception {
		String userAccessToken = loginAndGetAccessToken(USER_EMAIL);
		mockMvc.perform(get("/demo/admin").header("Authorization", "Bearer " + userAccessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("No tienes permisos para realizar esta operación."));

		String adminAccessToken = loginAndGetAccessToken("demo.admin@example.com");
		mockMvc.perform(get("/demo/admin").header("Authorization", "Bearer " + adminAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Acceso de administrador."));
	}

	@Test
	void demoAuthenticatedRouteRejectsExpiredAndTamperedAccessTokens() throws Exception {
		User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
		String expiredToken = jwtService.generateAccessToken(
				user, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
		mockMvc.perform(get("/demo/profile").header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized());

		String validToken = loginAndGetAccessToken(USER_EMAIL);
		String tamperedToken = validToken.substring(0, validToken.length() - 1)
				+ (validToken.endsWith("a") ? "b" : "a");
		mockMvc.perform(get("/demo/profile").header("Authorization", "Bearer " + tamperedToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void demoProfileStillAcceptsTokenAfterUserIsDisabled() throws Exception {
		// Limitación didáctica del JWT puro: el filtro no consulta la base de datos,
		// así que un token emitido sigue válido hasta que caduca aunque el usuario se deshabilite.
		String accessToken = loginAndGetAccessToken(USER_EMAIL);
		try {
			jdbcTemplate.update("UPDATE users SET enabled = 0 WHERE email = ?", USER_EMAIL);
			mockMvc.perform(get("/demo/profile").header("Authorization", "Bearer " + accessToken))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
		} finally {
			jdbcTemplate.update("UPDATE users SET enabled = 1 WHERE email = ?", USER_EMAIL);
		}
	}

	private String loginAndGetAccessToken(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType("application/json")
				.content("""
						{"email":"%s","password":"password"}
						""".formatted(email)))
			.andExpect(status().isOk())
			.andReturn();
		return extractToken(ACCESS_TOKEN_PATTERN, result.getResponse().getContentAsString());
	}

	private int countRows(String tableName) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
	}

	private String storedHash(String tableName) {
		return jdbcTemplate.queryForObject("SELECT HEX(token_hash) FROM " + tableName, String.class);
	}

	private String extractToken(Pattern pattern, String responseBody) {
		Matcher matcher = pattern.matcher(responseBody);
		assertTrue(matcher.find());
		return matcher.group(1);
	}

	private String hashAsHex(String token) throws Exception {
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(hash).toUpperCase(Locale.ROOT);
	}
}
