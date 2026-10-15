package es.cesguiro.login_security.persistence.dao.jdbc;

import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import es.cesguiro.login_security.domain.model.User;
import es.cesguiro.login_security.persistence.dao.UserDao;
import es.cesguiro.login_security.persistence.mapper.UserMapper;

@Repository
public class UserJdbcDao implements UserDao {

	private final JdbcTemplate jdbcTemplate;
	private final UserMapper userMapper;

	public UserJdbcDao(JdbcTemplate jdbcTemplate, UserMapper userMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.userMapper = userMapper;
	}

	@Override
	public Optional<User> findByEmail(String email) {
		String sql = "SELECT * FROM users WHERE email = ?";

		try {
			return Optional.of(jdbcTemplate.queryForObject(sql, userMapper, email));
		} catch (EmptyResultDataAccessException exception) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<User> findById(Long id) {
		String sql = "SELECT * FROM users WHERE id = ?";

		try {
			return Optional.of(jdbcTemplate.queryForObject(sql, userMapper, id));
		} catch (EmptyResultDataAccessException exception) {
			return Optional.empty();
		}
	}
}
