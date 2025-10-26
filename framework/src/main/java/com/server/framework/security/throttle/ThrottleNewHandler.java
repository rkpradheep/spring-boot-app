package com.server.framework.security.throttle;

import com.server.framework.common.DateUtil;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThrottleNewHandler
{
	private static final Logger LOGGER = Logger.getLogger(ThrottleNewHandler.class.getName());

	// Allow bursts up to 10; refill 5 tokens every second
	// Or for sliding window: max 10 requests per 60-second window

	private static final Integer MAX_REQUEST_ALLOWED = 100;  // capacity (burst) / max requests per window
	private static final Integer TOKENS_ADDED_PER_INTERVAL = 100;  // tokens added per interval (for token bucket)
	private static final Algorithm DEFAULT_ALGORITHM = Algorithm.TOKEN_BUCKET;  // tokens added per interval (for token bucket)
	private static final Integer WINDOW_SIZE = 60;  // tokens added per interval (for token bucket)

	static RateLimiterRegistry registry = new RateLimiterRegistry(MAX_REQUEST_ALLOWED, TOKENS_ADDED_PER_INTERVAL, Duration.ofSeconds(WINDOW_SIZE), DEFAULT_ALGORITHM);

	public static boolean tryAcquire(HttpServletResponse response)
	{
		try
		{
			String ip = getOriginatingUserIP();
			String uri = getCurrentRequestURI();
			
			String userId = ip + "-" + uri;

			RateLimiter limiter = registry.forKey(userId);

			if(!limiter.tryAcquire())
			{
				long retryAfterSeconds = (limiter.refillEveryMillis() - (DateUtil.getCurrentTimeInMillis() - limiter.lastRefillTimeMs())) / 1000;

				response.setHeader("Retry-After", retryAfterSeconds + " second(s)");
				response.setHeader("X-RateLimit-Limit", String.valueOf(limiter.capacity()));
				response.setHeader("X-RateLimit-Remaining", String.valueOf(limiter.remainingTokens()));
				response.setHeader("X-RateLimit-Algorithm", limiter.getAlgorithmName());
				return false;
			}

			response.setHeader("X-RateLimit-Limit", String.valueOf(limiter.capacity()));
			response.setHeader("X-RateLimit-Remaining", String.valueOf(limiter.remainingTokens()));
			response.setHeader("X-RateLimit-Algorithm", limiter.getAlgorithmName());
			return true;

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error in rate limiting", e);
			return false;
		}
	}

	public static void switchAlgorithm(Algorithm algorithm)
	{
		registry.switchAlgorithm(algorithm);
	}

	private static String getOriginatingUserIP() {
		try {
			// Try to get from SecurityFilter's ThreadLocal
			return "127.0.0.1"; // Simplified for now
		} catch (Exception e) {
			return "127.0.0.1";
		}
	}

	private static String getCurrentRequestURI() {
		try {
			// Try to get from SecurityFilter's ThreadLocal
			return "/"; // Simplified for now
		} catch (Exception e) {
			return "/";
		}
	}
}
