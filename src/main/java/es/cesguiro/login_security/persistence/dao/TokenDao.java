package es.cesguiro.login_security.persistence.dao;

import es.cesguiro.login_security.domain.model.StoredToken;

public interface TokenDao {

	void insertRefreshToken(StoredToken token);
}
