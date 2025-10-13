package com.server.framework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.framework.repository.LockRepository;

@Service
public class LockService
{
	public static final String COMMON_LOCK_NAME = "CommonLock";
	@Autowired LockRepository lockRepository;

	public void acquireLock(String lockName)
	{
		lockRepository.findByName(lockName);
	}

	public void acquireCommonLock()
	{
		lockRepository.findByName(COMMON_LOCK_NAME);
	}
}
