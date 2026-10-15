package es.cesguiro.login_security.persistence.dao;

import java.util.Set;

public interface RoleDao {

	Set<String> findNamesByUserId(Long userId);
}
