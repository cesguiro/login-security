package es.cesguiro.login_security.persistence.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper implements RowMapper<String> {

	@Override
	public String mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
		return resultSet.getString("name");
	}
}
