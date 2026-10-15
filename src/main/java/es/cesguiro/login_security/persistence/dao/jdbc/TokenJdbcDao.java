package es.cesguiro.login_security.persistence.dao.jdbc;

import java.sql.Timestamp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import es.cesguiro.login_security.domain.model.StoredToken;
import es.cesguiro.login_security.persistence.dao.TokenDao;

@Repository
public class TokenJdbcDao implements TokenDao {

	private final JdbcTemplate jdbcTemplate;

	public TokenJdbcDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void insertRefreshToken(StoredToken token) {
		String sql = """
				INSERT INTO refresh_tokens (user_id, token_hash, issued_at, expires_at)
				VALUES (?, ?, ?, ?)
				""";
		jdbcTemplate.update(sql,
				token.userId(), token.tokenHash(), Timestamp.from(token.issuedAt()), Timestamp.from(token.expiresAt()));
	}
}
