package com.server.framework.service;

import com.server.framework.entity.SessionManagementEntity;
import com.server.framework.repository.SessionManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SessionManagementService {
    
    @Autowired
    private SessionManagementRepository sessionManagementRepository;
    
    public SessionManagementEntity save(SessionManagementEntity session) {
        return sessionManagementRepository.save(session);
    }
    
    public Optional<SessionManagementEntity> findById(Long id) {
        return sessionManagementRepository.findById(id);
    }
    
    public Optional<SessionManagementEntity> findBySessionId(String sessionId) {
        return sessionManagementRepository.findBySessionId(sessionId);
    }
    
    public List<SessionManagementEntity> findByUserId(Long userId) {
        return sessionManagementRepository.findByUserId(userId);
    }
    
    public Optional<SessionManagementEntity> findByUserIdAndSessionId(Long userId, String sessionId) {
        return sessionManagementRepository.findByUserIdAndSessionId(userId, sessionId);
    }
    
    public List<SessionManagementEntity> findExpiredSessions(Long currentTime) {
        return sessionManagementRepository.findExpiredSessions(currentTime);
    }
    
    public Long countByUserId(Long userId) {
        return sessionManagementRepository.countByUserId(userId);
    }
    
    public void deleteByUserId(Long userId) {
        sessionManagementRepository.deleteByUserId(userId);
    }
    
    public void deleteBySessionId(String sessionId) {
        sessionManagementRepository.deleteBySessionId(sessionId);
    }
    
    public void deleteExpiredSessions(Long currentTime) {
        sessionManagementRepository.deleteExpiredSessions(currentTime);
    }
    
    public boolean existsBySessionId(String sessionId) {
        return sessionManagementRepository.existsBySessionId(sessionId);
    }
    
    public SessionManagementEntity createSession(Long userId, String sessionId, Long expiryTime) {
        SessionManagementEntity session = new SessionManagementEntity();
        session.setUserId(userId);
        session.setSessionId(sessionId);
        session.setExpiryTime(expiryTime);
        return save(session);
    }
    
    public boolean isSessionValid(String sessionId, Long currentTime) {
        Optional<SessionManagementEntity> session = findBySessionId(sessionId);
        return session.isPresent() && session.get().getExpiryTime() > currentTime;
    }
    
    public void cleanupExpiredSessions(Long currentTime) {
        deleteExpiredSessions(currentTime);
    }
}
