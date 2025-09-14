package com.server.framework.service;

import com.server.framework.common.DateUtil;
import com.server.framework.entity.SessionManagementEntity;
import com.server.framework.entity.UserEntity;
import com.server.framework.repository.SessionManagementRepository;
import com.server.framework.repository.UserRepository;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionManagementRepository sessionManagementRepository;

    public UserEntity validateCredentials(String name, String password) {
        if (name == null || password == null) return null;
        String hash = DigestUtils.sha256Hex(password.trim());
      return userRepository.findByNameAndPassword(name.trim(), hash).orElse(null);
    }

    public void addSession(String sessionId, Long userId, long expiryTimeInMillis) {
        SessionManagementEntity sm = new SessionManagementEntity();
        sm.setSessionId(sessionId);
        sm.setUserId(userId);
        sm.setExpiryTime(expiryTimeInMillis);
        sessionManagementRepository.save(sm);
    }

    public void deleteExpiredSessions() {
        long now = DateUtil.getCurrentTimeInMillis();
        sessionManagementRepository.deleteExpiredSessions(now);
    }
}
