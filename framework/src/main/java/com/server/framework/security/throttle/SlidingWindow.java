package com.server.framework.security.throttle;

import com.server.framework.common.DateUtil;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

final class SlidingWindow implements RateLimiter
{
	private final long maxRequests;
	private final long windowSizeMillis;
	private final ConcurrentLinkedQueue<Long> requestTimestamps;
	private final AtomicLong lastCleanupTime;

	SlidingWindow(long maxRequests, long windowSizeMillis)
	{
		this.maxRequests = maxRequests;
		this.windowSizeMillis = windowSizeMillis;
		this.requestTimestamps = new ConcurrentLinkedQueue<>();
		this.lastCleanupTime = new AtomicLong(DateUtil.getCurrentTimeInMillis());
	}

	@Override
	public boolean tryAcquire()
	{
		long now = DateUtil.getCurrentTimeInMillis();

		cleanupExpiredTimestamps(now);

		if(requestTimestamps.size() < maxRequests)
		{
			requestTimestamps.offer(now);
			return true;
		}

		return false;
	}

	@Override
	public long remainingTokens()
	{
		cleanupExpiredTimestamps(DateUtil.getCurrentTimeInMillis());
		return Math.max(0, maxRequests - requestTimestamps.size());
	}

	@Override
	public long refillEveryMillis()
	{
		return windowSizeMillis;
	}

	@Override
	public long capacity()
	{
		return maxRequests;
	}

	@Override
	public long lastRefillTimeMs()
	{
		return lastCleanupTime.get();
	}

	@Override
	public String getAlgorithmName()
	{
		return "Sliding Window";
	}

	private void cleanupExpiredTimestamps(long now)
	{
		// performance optimization
		if(now - lastCleanupTime.get() < windowSizeMillis / 10)
		{
			return;
		}

		long cutoffTime = now - windowSizeMillis;
		while(!requestTimestamps.isEmpty())
		{
			Long timestamp = requestTimestamps.peek();
			if(timestamp != null && timestamp < cutoffTime)
			{
				requestTimestamps.poll();
			}
			else
			{
				break;
			}
		}

		lastCleanupTime.set(now);
	}
}
