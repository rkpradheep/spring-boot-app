package com.server.zoho.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.server.zoho.entity.BuildMonitorEntity;

import com.server.zoho.repository.BuildMonitorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class BuildMonitorService
{

	private static final Logger LOGGER = Logger.getLogger(BuildMonitorService.class.getName());

	@Autowired
	private BuildMonitorRepository buildMonitorRepository;

	@Autowired
	private BuildProductService buildProductService;

	private final ObjectMapper objectMapper;

	public BuildMonitorService()
	{
		this.objectMapper = new ObjectMapper();
		this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	public BuildMonitorEntity createBuildMonitor(List<String> productNames)
	{
		try
		{
			BuildMonitorEntity monitor = new BuildMonitorEntity(productNames.size());
			BuildMonitorEntity saved = buildMonitorRepository.save(monitor);

			buildProductService.createBuildProducts(saved.getId(), productNames);

			LOGGER.info("Created build monitor with ID: " + saved.getId() + " and " + productNames.size() + " products");
			return saved;

		}
		catch(Exception e)
		{
			LOGGER.severe("Failed to create build monitor: " + e.getMessage());
			throw new RuntimeException("Failed to create build monitor", e);
		}
	}

	public BuildMonitorEntity save(BuildMonitorEntity monitor)
	{
		return buildMonitorRepository.save(monitor);
	}

	public void updateLastUpdateTime(BuildMonitorEntity monitor)
	{
		monitor.updateLastUpdateTime();
		buildMonitorRepository.save(monitor);
	}

	public void markAsCompleted(BuildMonitorEntity monitor)
	{
		monitor.setStatus("COMPLETED");
		monitor.updateLastUpdateTime();
		buildMonitorRepository.save(monitor);
		LOGGER.info("BuildMonitor " + monitor.getId() + " marked as completed");
	}

	public void markFailed(BuildMonitorEntity monitor, String reason)
	{
		monitor.setStatus("FAILED");
		monitor.updateLastUpdateTime();
		buildMonitorRepository.save(monitor);
		LOGGER.warning("Build monitor failed: " + reason + " for monitor ID: " + monitor.getId());
	}

	public List<BuildMonitorEntity> getActiveMonitors()
	{
		return buildMonitorRepository.findByStatus("ACTIVE");
	}

	public List<BuildMonitorEntity> getRecentBuildMonitors()
	{
		return buildMonitorRepository.findTop10ByOrderByStartTimeDesc();
	}

	public Optional<BuildMonitorEntity> getById(Long id)
	{
		return buildMonitorRepository.findById(id);
	}

	public void deleteById(Long monitorId)
	{
		try
		{
			LOGGER.info("Deleting BuildMonitor with ID: " + monitorId);
			buildMonitorRepository.deleteById(monitorId);
			LOGGER.info("BuildMonitor deleted successfully: " + monitorId);
		}
		catch(Exception e)
		{
			LOGGER.severe("Failed to delete BuildMonitor " + monitorId + ": " + e.getMessage());
			throw new RuntimeException("Failed to delete BuildMonitor", e);
		}
	}

}
