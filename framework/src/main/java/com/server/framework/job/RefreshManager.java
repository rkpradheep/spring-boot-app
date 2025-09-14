package com.server.framework.job;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.server.framework.common.AppProperties;
import com.server.framework.common.CustomThreadFactory;
import com.server.framework.common.DateUtil;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class RefreshManager
{

	private static final Logger LOGGER = Logger.getLogger(RefreshManager.class.getName());

	private static ThreadPoolExecutor executor = null;
	private static DelayQueue queue;

	@PostConstruct
	public void init()
	{
		LOGGER.info("Starting Refresh Manager with thread count: " + AppProperties.getProperty("job.thread.count"));
		queue = new DelayQueue<>();
		int threadCount = Integer.parseInt(AppProperties.getProperty("job.thread.count"));
		executor = new ThreadPoolExecutor(threadCount, threadCount, 1L, TimeUnit.MINUTES, queue, new CustomThreadFactory("refresh-manager-"), new ThreadPoolExecutor.CallerRunsPolicy());
		executor.prestartAllCoreThreads();
	}

	@PreDestroy
	public void shutDown()
	{
		if(Objects.nonNull(executor))
		{
			executor.shutdownNow();
		}
	}

	public static void addJobInQueue(CustomRunnable runnable, long millisecondDelay)
	{
		queue.add(new RefreshElement(runnable, DateUtil.getCurrentTimeInMillis() + millisecondDelay));
	}

	private static class RefreshElement implements Delayed, Runnable
	{
		CustomRunnable runnable;
		long timeDelay;

		RefreshElement(CustomRunnable runnable, long millisSecondsDelay)
		{
			this.runnable = runnable;
			this.timeDelay = millisSecondsDelay;
		}

		@Override
		public void run()
		{
			try
			{
				LOGGER.info("Job dispatched for runnable " + runnable);
				runnable.run();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.INFO, "Exception during refresh job", e);
			}
		}

		@Override
		public int compareTo(Delayed delayed)
		{
			long delayedTime = ((RefreshElement) delayed).timeDelay;
			return Long.compare(timeDelay, delayedTime);
		}

		@Override
		public long getDelay(TimeUnit tu)
		{
			long delay = timeDelay - DateUtil.getCurrentTimeInMillis();
			return tu.convert(delay, TimeUnit.MILLISECONDS);
		}
	}
}
