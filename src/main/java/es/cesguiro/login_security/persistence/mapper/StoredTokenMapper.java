package es.cesguiro.login_security.persistence.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import es.cesguiro.login_security.domain.model.StoredToken;

@Component
public class StoredTokenMapper implements RowMapper<StoredToken> {

	@Override
	public StoredToken mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
		return new StoredToken(
				resultSet.getLong("user_id"),
				resultSet.getBytes("token_hash"),
				resultSet.getTimestamp("issued_at").toInstant(),
				resultSet.getTimestamp("expires_at").toInstant());
	}
}
