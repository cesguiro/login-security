package es.cesguiro.login_security.persistence.repository.impl;

import org.springframework.stereotype.Repository;

import es.cesguiro.login_security.domain.model.StoredToken;
import es.cesguiro.login_security.domain.repository.TokenRepository;
import es.cesguiro.login_security.persistence.dao.TokenDao;

@Repository
public class TokenRepositoryImpl implements TokenRepository {

	private final TokenDao tokenDao;

	public TokenRepositoryImpl(TokenDao tokenDao) {
		this.tokenDao = tokenDao;
	}

	@Override
	public void saveRefreshToken(StoredToken refreshToken) {
		tokenDao.insertRefreshToken(refreshToken);
	}
}
