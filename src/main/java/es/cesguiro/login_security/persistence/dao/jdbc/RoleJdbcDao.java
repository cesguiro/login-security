package es.cesguiro.login_security.persistence.dao.jdbc;

import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import es.cesguiro.login_security.persistence.dao.RoleDao;
import es.cesguiro.login_security.persistence.mapper.RoleMapper;

@Repository
public class RoleJdbcDao implements RoleDao {

	private final JdbcTemplate jdbcTemplate;
	private final RoleMapper roleMapper;

	public RoleJdbcDao(JdbcTemplate jdbcTemplate, RoleMapper roleMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.roleMapper = roleMapper;
	}

	@Override
	public Set<String> findNamesByUserId(Long userId) {
		String sql = """
				SELECT roles.name
				FROM roles
			JOIN user_roles ON user_roles.role_id = roles.id
			WHERE user_roles.user_id = ?
			ORDER BY roles.name
			""";

		return Set.copyOf(jdbcTemplate.query(sql, roleMapper, userId));
	}
}
