package com.server.framework.service;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.framework.common.AppProperties;
import com.server.framework.common.DateUtil;
import com.server.framework.entity.JobEntity;
import com.server.framework.job.JobStatus;
import com.server.framework.job.Task;
import com.server.framework.job.TaskEnum;
import com.server.framework.repository.JobRepository;

@Component
public class JobWrapper
{
	@Autowired
	private JobService jobService;
	@Autowired
	private JobRepository jobRepository;

	private static final Logger LOGGER = Logger.getLogger(JobWrapper.class.getName());

	void executeJob(JobEntity jobEntity)
	{

		LOGGER.info("Executing job: " + jobEntity.getId() + " - " + jobEntity.getTaskName());
		Task task = null;

		try
		{
			jobEntity.setStatus(JobStatus.JOB_RUNNING);
			jobRepository.save(jobEntity);

			task = TaskEnum.getTaskInstance(jobEntity.getTaskName());
			task.run(jobEntity.getId());

			LOGGER.info("Job " + jobEntity.getId() + " executed successfully");
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error executing job task: " + jobEntity.getId(), e);
		}
		finally
		{
			handleJobCompletion(task, jobEntity);
		}
	}

	private void handleJobCompletion(Task task, JobEntity jobEntity)
	{
		try
		{
			jobEntity.setStatus(JobStatus.JOB_NOT_RUNNING);
			jobRepository.save(jobEntity);

			if(Boolean.TRUE.equals(jobEntity.getIsRecurring()) && jobEntity.getDayInterval() != null && jobEntity.getDayInterval() > 0)
			{
				long nextExecutionTime = jobService.getNextExecutionTimeFromPreviousScheduleTime(jobEntity.getScheduledTime(), jobEntity.getDayInterval());

				jobService.updateNextExecutionTime(jobEntity.getId(), nextExecutionTime);

				LOGGER.info("Recurring job " + jobEntity.getId() + " rescheduled for: " + new java.util.Date(nextExecutionTime));

				if(!AppProperties.getBoolean("job.dispatcher.enabled"))
				{
					jobService.scheduleJob(jobEntity);
				}

				return;
			}

			if(Objects.nonNull(task) && task.canAddJobAgain())
			{
				long nextExecutionTime = DateUtil.getCurrentTimeInMillis() + task.getDelayBeforeAddJobAgain();

				jobService.updateNextExecutionTime(jobEntity.getId(), nextExecutionTime);

				LOGGER.info("Job " + jobEntity.getId() + " rescheduled for: " + DateUtil.getFormattedTime(nextExecutionTime, DateUtil.DATE_WITH_TIME_SECONDS_FORMAT));

				if(!AppProperties.getBoolean("job.dispatcher.enabled"))
				{
					jobService.scheduleJob(jobEntity);
				}
			}
			else
			{
				jobService.deleteJob(jobEntity.getId());
				LOGGER.info("One-time job " + jobEntity.getId() + " completed and deleted");
			}

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error handling job completion", e);
		}
	}
}
