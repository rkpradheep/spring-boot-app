package com.server.framework.service;

import com.server.framework.common.AppProperties;
import com.server.framework.common.DateUtil;
import com.server.framework.entity.JobEntity;
import com.server.framework.job.CustomRunnable;
import com.server.framework.job.RefreshManager;
import com.server.framework.repository.JobRepository;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JobService
{
	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private ConfigurationService configurationService;

	@Lazy
	@Autowired
	private JobWrapper jobWrapper;

	public List<JobEntity> getAllJobs()
	{
		return jobRepository.findAll();
	}

	public void scheduleJob(CustomRunnable runnable, int seconds)
	{
		scheduleJob(runnable, seconds * 1000L);
	}

	public void scheduleJob(CustomRunnable runnable, long millisecondDelay)
	{
		RefreshManager.addJobInQueue(runnable, millisecondDelay);
	}

	public void scheduleJob(JobEntity jobEntity)
	{
		jobRepository.save(jobEntity);
		RefreshManager.addJobInQueue(() -> jobWrapper.executeJob(jobEntity), Math.min(0, jobEntity.getScheduledTime() - DateUtil.getCurrentTimeInMillis()));
	}

	public List<JobEntity> findJobsToExecute(Long executionTime)
	{
		return jobRepository.findJobsToExecute(executionTime);
	}

	public Long scheduleJob(String taskName, String data, long executionTimeInMilliseconds) throws Exception
	{
		return scheduleJob(taskName, data, executionTimeInMilliseconds, 0, false);
	}

	public Long scheduleJob(String taskName, String data, long executionTimeInMilliseconds, int dayInterval, boolean isRecurring) throws Exception
	{
		JobEntity jobEntity = new JobEntity();
		jobEntity.setTaskName(taskName);
		jobEntity.setData(data);
		jobEntity.setScheduledTime(executionTimeInMilliseconds);
		jobEntity.setDayInterval(dayInterval);
		jobEntity.setIsRecurring(isRecurring);
		Long jobID = jobRepository.save(jobEntity).getId();

		if(!AppProperties.getBoolean("job.dispatcher.enabled"))
		{
			scheduleJob(jobEntity);
		}

		return jobID;
	}

	public Optional<JobEntity> getJob(long jobId)
	{
		return jobRepository.findById(jobId);
	}

	public void updateNextExecutionTime(long jobId, long nextExecutionTime)
	{
		jobRepository.findById(jobId).ifPresent(j -> {
			j.setScheduledTime(nextExecutionTime);
			jobRepository.save(j);
		});
	}

	public void deleteJob(long id)
	{
		if(id == -1)
			return;
		jobRepository.deleteById(id);
		configurationService.delete(DigestUtils.sha1Hex(String.valueOf(id)));
	}

	public long getNextExecutionTimeFromPreviousScheduleTime(long previousExecution, int dayInterval)
	{
		long nextExecutionTime = previousExecution + (DateUtil.ONE_DAY_IN_MILLISECOND * dayInterval);
		while(nextExecutionTime < DateUtil.getCurrentTimeInMillis())
		{
			nextExecutionTime += (DateUtil.ONE_DAY_IN_MILLISECOND * dayInterval);
		}
		return nextExecutionTime;
	}

	public long getNextExecutionDelayTimeFromStartDate(String startDate, String startDateFormat, long dayInterval, String timeOfDay)
	{
		int hour = Integer.parseInt(timeOfDay.split(":")[0]);
		int minute = Integer.parseInt(timeOfDay.split(":")[1]);
		long millis = DateUtil.getDayStartInMillis(startDate, startDateFormat);
		long nextExecutionTime = millis + DateUtil.getDelayFromDayStart(hour, minute);
		while(nextExecutionTime < DateUtil.getCurrentTimeInMillis())
		{
			nextExecutionTime += (DateUtil.ONE_DAY_IN_MILLISECOND * dayInterval);
		}
		return nextExecutionTime;
	}

}
