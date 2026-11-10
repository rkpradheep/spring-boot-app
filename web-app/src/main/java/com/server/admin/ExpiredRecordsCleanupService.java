package com.server.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.framework.common.DateUtil;
import com.server.framework.repository.ConfigurationRepository;
import com.server.framework.repository.HttpLogRepository;
import com.server.framework.repository.JobRepository;
import com.server.framework.repository.SessionManagementRepository;
import com.server.framework.repository.WorkflowEventRepository;
import com.server.framework.repository.WorkflowInstanceRepository;
import com.server.zoho.repository.BuildMonitorRepository;

@Service
public class ExpiredRecordsCleanupService
{
	private static final Logger logger = LoggerFactory.getLogger(ExpiredRecordsCleanupService.class);

	private static final long CREATED_TIME_RETENTION_MILLIS = 15 * DateUtil.ONE_DAY_IN_MILLISECOND;

	@Autowired private ConfigurationRepository configurationRepository;
	@Autowired private SessionManagementRepository sessionManagementRepository;
	@Autowired private HttpLogRepository httpLogRepository;
	@Autowired private WorkflowInstanceRepository workflowInstanceRepository;
	@Autowired private WorkflowEventRepository workflowEventRepository;
	@Autowired private JobRepository jobRepository;
	@Autowired private BuildMonitorRepository buildMonitorRepository;

	@Transactional
	public Map<String, Integer> cleanupExpiredRecords()
	{
		long now = DateUtil.getCurrentTimeInMillis();
		long createdTimeCutoff = now - CREATED_TIME_RETENTION_MILLIS;

		Map<String, Integer> deleted = new LinkedHashMap<>();
		deleted.put("Configuration", configurationRepository.deleteExpired(now));
		deleted.put("SessionManagement", sessionManagementRepository.deleteExpiredSessions(now));
		deleted.put("WorkflowEvent", workflowEventRepository.deleteOlderThan(createdTimeCutoff));
		deleted.put("WorkflowInstance", workflowInstanceRepository.deleteOlderThan(createdTimeCutoff));
		deleted.put("Job", jobRepository.deleteOlderThan(createdTimeCutoff));
		deleted.put("BuildMonitor", buildMonitorRepository.deleteOlderThan(createdTimeCutoff));
		deleted.put("HttpLog", httpLogRepository.deleteOldLogs(createdTimeCutoff));

		logger.info("Expired records cleanup completed :: {}", deleted);
		return deleted;
	}

	@Transactional
	@EventListener(ApplicationReadyEvent.class)
	public void cleanupOnStartup()
	{
		try
		{
			cleanupExpiredRecords();
		}
		catch(Exception e)
		{
			logger.error("Expired records cleanup on startup failed", e);
		}
	}
}
