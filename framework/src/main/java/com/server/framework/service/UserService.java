package com.server.framework.service;

import com.server.framework.entity.AuthTokenEntity;
import com.server.framework.entity.SessionManagementEntity;
import com.server.framework.entity.UserEntity;
import com.server.framework.repository.AuthTokenRepository;
import com.server.framework.repository.SessionManagementRepository;
import com.server.framework.repository.UserRepository;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private SessionManagementRepository sessionRepository;

    public Optional<UserEntity> findByName(String name) {
        return userRepository.findAll().stream().filter(u -> name != null && name.equalsIgnoreCase(u.getName())).findFirst();
    }

    public Optional<UserEntity> findByToken(String token) {
        Optional<AuthTokenEntity> tok = authTokenRepository.findByToken(token);
		return tok.map(AuthTokenEntity::getUser);
	}

    public Optional<UserEntity> findBySession(String sessionId) {
        Optional<SessionManagementEntity> sm = sessionRepository.findBySessionId(sessionId);
        if (sm.isEmpty()) return Optional.empty();
        return userRepository.findById(sm.get().getUserId());
    }

    public java.util.List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    public UserEntity createUser(String name, String password, Integer role) {
        UserEntity userEntity = new UserEntity();
        userEntity.setName(name);
        userEntity.setPassword(password);
        userEntity.setRoleType(role);
        return userRepository.save(userEntity);
    }

    public UserEntity updateUser(long id, String name, String password, Integer role) {
        UserEntity userEntity = userRepository.findById(id).orElseThrow();
        userEntity.setName(name);
        userEntity.setPassword(DigestUtils.sha256Hex(password.trim()));
        userEntity.setRoleType(role);
        return userRepository.save(userEntity);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
