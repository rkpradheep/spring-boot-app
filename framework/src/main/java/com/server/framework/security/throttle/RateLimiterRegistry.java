package com.server.framework.security.throttle;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.server.framework.common.AppProperties;
import com.server.framework.security.SecurityUtil;


final class RateLimiterRegistry
{
	final Map<String, RateLimiter> buckets = new ConcurrentHashMap<>();
	final long capacity;
	final long refillTokens;
	final long refillIntervalMillis;
	Algorithm algorithm;

	RateLimiterRegistry(long capacity, long refillTokens, Duration refillInterval, Algorithm algorithm)
	{
		this.capacity = capacity;
		this.refillTokens = refillTokens;
		this.refillIntervalMillis = refillInterval.toMillis();
		this.algorithm = algorithm;
	}

	RateLimiter forKey(String key)
	{
		long customerCapacity = SecurityUtil.isRequestFromLoopBackAddress() ? 1000 : AppProperties.getProperty("environment").equals("zoho") ? 100 : capacity;
		long customerRefillTokens = SecurityUtil.isRequestFromLoopBackAddress() ? 1000: AppProperties.getProperty("environment").equals("zoho") ? 100 : refillTokens;
		return buckets.computeIfAbsent(key, k -> {
			switch(algorithm)
			{
				case SLIDING_WINDOW:
					return new SlidingWindow(customerCapacity, refillIntervalMillis);
				default:
					return new TokenBucket(customerCapacity, customerRefillTokens, refillIntervalMillis);
			}
		});
	}

	public void switchAlgorithm(Algorithm algorithm)
	{
		buckets.clear();
		this.algorithm = algorithm;
	}
}
