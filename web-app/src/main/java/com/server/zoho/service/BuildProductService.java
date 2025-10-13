package com.server.zoho.service;

import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.repository.BuildProductRepository;
import com.server.zoho.workflow.model.BuildProductStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class BuildProductService
{

	private static final Logger LOGGER = Logger.getLogger(BuildProductService.class.getName());

	@Autowired
	private BuildProductRepository buildProductRepository;

	public void createBuildProducts(Long buildMonitorId, List<String> productNames)
	{
		List<BuildProductEntity> buildProductEntities = new java.util.ArrayList<>();

		for(int i = 0; i < productNames.size(); i++)
		{
			BuildProductEntity buildProductEntity = new BuildProductEntity(buildMonitorId, productNames.get(i), i + 1);
			buildProductEntities.add(buildProductRepository.save(buildProductEntity));
		}

		LOGGER.info("Created " + buildProductEntities.size() + " BuildProduct entries for BuildMonitor " + buildMonitorId);
	}

	public Optional<BuildProductEntity> getNextPendingProduct(Long buildMonitorId)
	{
		return buildProductRepository.findNextPendingProduct(buildMonitorId);
	}

	public Optional<BuildProductEntity> getCurrentInProgressProduct(Long buildMonitorId)
	{
		return buildProductRepository.findCurrentInProgressProduct(buildMonitorId);
	}

	public Optional<BuildProductEntity> getCurrentActiveProduct(Long buildMonitorId)
	{
		return buildProductRepository.findCurrentActiveProduct(buildMonitorId);
	}

	public Optional<BuildProductEntity> getById(Long productId)
	{
		return buildProductRepository.findById(productId);
	}

	public List<BuildProductEntity> getProductsForMonitor(Long buildMonitorId)
	{
		return buildProductRepository.findByBuildMonitorIdOrderByOrderIndexAsc(buildMonitorId);
	}

	public void startBuild(BuildProductEntity buildProductEntity, Long buildId)
	{
		buildProductEntity.markAsStarted(buildId);
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("Started build " + buildId + " for product " + buildProductEntity.getProductName());
	}

	public void markBuildSuccess(BuildProductEntity buildProductEntity)
	{
		buildProductEntity.markAsSuccess();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("Marked build " + buildProductEntity.getBuildId() + " as successful for product " + buildProductEntity.getProductName());
	}

	public void markBuildFailed(BuildProductEntity buildProductEntity, String errorMessage)
	{
		buildProductEntity.markAsFailed(errorMessage);
		buildProductRepository.save(buildProductEntity);
		LOGGER.warning("Marked build " + buildProductEntity.getBuildId() + " as failed for product " + buildProductEntity.getProductName() + ": " + errorMessage);
	}

	public void markBuildFailed(BuildProductEntity buildProductEntity, String status, String apiErrorResponse)
	{
		buildProductEntity.setStatus(status);
		buildProductEntity.setErrorMessage(apiErrorResponse);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.warning("Marked build " + buildProductEntity.getBuildId() + " as " + status + " for product " + buildProductEntity.getProductName() + ": " + apiErrorResponse);
	}

	public void markMilestoneCreated(BuildProductEntity buildProductEntity, String milestoneVersion)
	{
		buildProductEntity.setStatus("MILESTONE_CREATED");
		buildProductEntity.setErrorMessage(null);
		buildProductEntity.setMilestoneVersion(milestoneVersion);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as MILESTONE_CREATED with version: " + milestoneVersion);
	}

	public void markChannelMapped(BuildProductEntity buildProductEntity, String channelUrl)
	{
		buildProductEntity.setStatus(BuildProductStatus.CHANNEL_MAPPED.getName());
		buildProductEntity.setChannelUrl(channelUrl);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as CHANNEL_MAPPED with URL: " + channelUrl);
	}

	public BuildProductEntity save(BuildProductEntity buildProductEntity)
	{
		return buildProductRepository.save(buildProductEntity);
	}

	public void markBuildStarted(BuildProductEntity buildProductEntity, Long buildId)
	{
		buildProductEntity.markAsStarted(buildId);
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as STARTED");
	}

}
