package com.server.framework.security.throttle;

interface RateLimiter
{
	boolean tryAcquire();

	long remainingTokens();

	long refillEveryMillis();

	long capacity();

	long lastRefillTimeMs();

	String getAlgorithmName();
}
