package es.cesguiro.login_security.domain.repository;

import es.cesguiro.login_security.domain.model.StoredToken;

public interface TokenRepository {

	void saveRefreshToken(StoredToken refreshToken);
}
