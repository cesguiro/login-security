package es.cesguiro.login_security.persistence.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import es.cesguiro.login_security.domain.model.User;

@Component
public class UserMapper implements RowMapper<User> {

	@Override
	public User mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
		return new User(
				resultSet.getLong("id"),
				resultSet.getString("email"),
				resultSet.getString("display_name"),
				resultSet.getString("password_hash"),
				resultSet.getBoolean("enabled"),
				Set.of());
	}
}
