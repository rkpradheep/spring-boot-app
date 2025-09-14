package com.server.framework.job;

import com.server.framework.common.AppProperties;
import com.server.framework.common.CustomThreadFactory;
import com.server.framework.common.DateUtil;
import com.server.framework.entity.JobEntity;
import com.server.framework.repository.JobRepository;
import com.server.framework.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JobDispatcher
{

	private static final Logger LOGGER = Logger.getLogger(JobDispatcher.class.getName());

	@Autowired
	private JobService jobService;

	@Autowired
	private JobRepository jobRepository;

	private ScheduledExecutorService scheduler;

	@PostConstruct
	public void initialize()
	{
		try
		{
			jobRepository.markAllJobsAsNotRunning();

			String canAddJobDispatcher = AppProperties.getProperty("job.dispatcher.enabled");
			if("false".equals(canAddJobDispatcher))
			{
				LOGGER.info("Job dispatcher is disabled in configuration");
				getAllJobsFromDBAndSchedule();
				return;
			}

			int threadCount = AppProperties.getIntProperty("job.thread.count", 2);

			scheduler = Executors.newScheduledThreadPool(1, new CustomThreadFactory("job-dispatcher-"));

			scheduler.scheduleWithFixedDelay(this::pollAndExecuteJobs, 10, AppProperties.getIntProperty("job.dispatcher.running.interval.seconds", 300), TimeUnit.SECONDS);

			LOGGER.info("Job dispatcher initialized with " + threadCount + " threads");

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to initialize job dispatcher", e);
		}
	}

	@PreDestroy
	public void shutdown()
	{
		try
		{
			if(scheduler == null)
			{
				return;
			}

			scheduler.shutdown();
			if(!scheduler.awaitTermination(10, TimeUnit.SECONDS))
			{
				scheduler.shutdownNow();
			}

			LOGGER.info("Job dispatcher shut down successfully");

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error shutting down job dispatcher", e);
		}
	}

	private void pollAndExecuteJobs()
	{
		try
		{
			int lookAheadSeconds = AppProperties.getIntProperty("job.dispatcher.look.ahead.seconds", 300);
			List<JobEntity> jobsToExecute = jobService.findJobsToExecute(DateUtil.getCurrentTimeInMillis() + (DateUtil.ONE_SECOND_IN_MILLISECOND * lookAheadSeconds));

			if(!jobsToExecute.isEmpty())
			{
				LOGGER.info("Found " + jobsToExecute.size() + " jobs to execute");
			}

			for(JobEntity jobEntity : jobsToExecute)
			{
				jobService.scheduleJob(jobEntity);
			}

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error polling jobs", e);
		}
	}

	private void getAllJobsFromDBAndSchedule()
	{
		try
		{
			List<JobEntity> allJobs = jobService.getAllJobs();

			for(JobEntity jobEntity : allJobs)
			{
				jobService.scheduleJob(jobEntity);
			}
			LOGGER.info("Scheduled " + allJobs.size() + " jobs from database");
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error scheduling jobs from database", e);
		}
	}
}


