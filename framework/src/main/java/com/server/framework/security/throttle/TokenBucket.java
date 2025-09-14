package com.server.framework.security.throttle;

import java.util.concurrent.atomic.AtomicLong;

import com.server.framework.common.DateUtil;

final class TokenBucket implements RateLimiter
{
	private final long capacity;
	private final long refillTokens;
	private final long refillIntervalMillis;

	// we keep tokens in a double to allow fractional accumulation when intervals are short
	private volatile double tokens;
	private final AtomicLong lastRefillTimeMs = new AtomicLong();

	TokenBucket(long capacity, long refillTokens, long refillIntervalMillis)
	{
		this.capacity = capacity;
		this.refillTokens = refillTokens;
		this.refillIntervalMillis = refillIntervalMillis;
		this.tokens = capacity; // start full (common choice)
		this.lastRefillTimeMs.set(DateUtil.getCurrentTimeInMillis());
	}

	@Override
	public synchronized boolean tryAcquire()
	{
		refill();
		if(tokens >= 1.0)
		{
			tokens -= 1.0;
			return true;
		}
		return false;
	}

	@Override
	public synchronized long remainingTokens()
	{
		refill();
		return (long) Math.floor(tokens);
	}

	@Override
	public long refillEveryMillis()
	{
		return refillIntervalMillis;
	}

	@Override
	public long capacity()
	{
		return capacity;
	}

	@Override
	public long lastRefillTimeMs()
	{
		return lastRefillTimeMs.get();
	}

	@Override
	public String getAlgorithmName()
	{
		return "Token Bucket";
	}

	synchronized private void refill()
	{
		long now = DateUtil.getCurrentTimeInMillis();
		long elapsed = now - lastRefillTimeMs.get();
		if(elapsed <= 0)
			return;

		long intervals = elapsed / refillIntervalMillis;
		if(intervals > 0)
		{
			double toAdd = intervals * (double) refillTokens;
			tokens = Math.min(capacity, tokens + toAdd);
			lastRefillTimeMs.addAndGet(intervals * refillIntervalMillis);
		}
	}
}
