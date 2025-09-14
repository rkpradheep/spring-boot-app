package com.server.framework.service;

import com.server.framework.entity.AuthTokenEntity;
import com.server.framework.entity.UserEntity;
import com.server.framework.repository.AuthTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthTokenService {
    
    @Autowired
    private AuthTokenRepository authTokenRepository;
    
    public AuthTokenEntity save(AuthTokenEntity authTokenEntity) {
        return authTokenRepository.save(authTokenEntity);
    }
    
    public Optional<AuthTokenEntity> findById(Long id) {
        return authTokenRepository.findById(id);
    }
    
    public Optional<AuthTokenEntity> findByToken(String token) {
        return authTokenRepository.findByToken(token);
    }

    public void deleteByUser(UserEntity userEntity) {
        authTokenRepository.deleteByUser(userEntity);
    }
    
    public void deleteByToken(String token) {
        authTokenRepository.deleteByToken(token);
    }
    
    public boolean existsByToken(String token) {
        return authTokenRepository.existsByToken(token);
    }
    
    public void createToken(UserEntity userEntity, String token) {
        AuthTokenEntity authTokenEntity = new AuthTokenEntity();
        authTokenEntity.setUser(userEntity);
        authTokenEntity.setToken(token);
        save(authTokenEntity);
    }
    
    public boolean validateToken(String token) {
        return findByToken(token).isPresent();
    }
    
    public void revokeUserTokens(UserEntity userEntity) {
        deleteByUser(userEntity);
    }
}
