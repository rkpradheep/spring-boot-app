package com.server.zoho.service;

import com.server.zoho.IntegService;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.repository.BuildProductRepository;
import com.server.zoho.workflow.model.BuildProductStatus;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Service
@Transactional
public class BuildProductService
{

	private static final Logger LOGGER = Logger.getLogger(BuildProductService.class.getName());

	@Autowired
	private BuildProductRepository buildProductRepository;

	public void createBuildProducts(Long buildMonitorId, Set<String> productNames)
	{
		List<String> finalizedProductNamesForBuild = new ArrayList<>(productNames);
		for(String productName : productNames)
		{
			String parentProduct = IntegService.getProductConfig(productName).getParentProduct();
			if(!finalizedProductNamesForBuild.contains(productName) && StringUtils.isNotEmpty(parentProduct) && !finalizedProductNamesForBuild.contains(parentProduct))
			{
				finalizedProductNamesForBuild.add(parentProduct);
			}
		}
		List<BuildProductEntity> buildProductEntities = new java.util.ArrayList<>();
		finalizedProductNamesForBuild.sort(Comparator.comparingInt(productName-> IntegService.getProductConfig(productName).getOrder()));

		for(int i = 0; i < finalizedProductNamesForBuild.size(); i++)
		{
			BuildProductEntity buildProductEntity = new BuildProductEntity(buildMonitorId, finalizedProductNamesForBuild.get(i), i + 1);
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
		buildProductEntity.setStatus(BuildProductStatus.MILESTONE_CREATED.getName());
		buildProductEntity.setErrorMessage(null);
		buildProductEntity.setMilestoneVersion(milestoneVersion);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as MILESTONE_CREATED with version: " + milestoneVersion);
	}

	public void markMilestoneFailed(BuildProductEntity buildProductEntity, String errorMessage)
	{
		if(Objects.isNull(buildProductEntity))
		{
			LOGGER.warning("Cannot mark milestone failed. BuildProductEntity is null.");
			return;
		}
		buildProductEntity.setStatus(BuildProductStatus.MILESTONE_FAILED.getName());
		buildProductEntity.setErrorMessage(errorMessage);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as MILESTONE_FAILED");
	}

	public void markChannelMapped(BuildProductEntity buildProductEntity, String channelUrl)
	{
		buildProductEntity.setStatus(BuildProductStatus.CHANNEL_MAPPED.getName());
		buildProductEntity.setChannelUrl(channelUrl);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as CHANNEL_MAPPED with URL: " + channelUrl);
	}

	public void markChannelMappingFailed(BuildProductEntity buildProductEntity, String errorMessage)
	{
		if(Objects.isNull(buildProductEntity))
		{
			LOGGER.warning("Cannot mark channel mapping failed. BuildProductEntity is null.");
			return;
		}
		buildProductEntity.setStatus(BuildProductStatus.CHANNEL_MAPPING_FAILED.getName());
		buildProductEntity.setErrorMessage(errorMessage);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as CHANNEL_MAPPING_FAILED");
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

	public void markSDCSEZBuildUploaded(BuildProductEntity buildProductEntity)
	{
		buildProductEntity.setStatus(BuildProductStatus.SD_CSEZ_BUILD_UPLOADED.getName());
		buildProductEntity.setErrorMessage(null);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as SD_BUILD_UPLOADED");
	}

	public void markSDLocalBuildUploaded(BuildProductEntity buildProductEntity)
	{
		buildProductEntity.setStatus(BuildProductStatus.SD_LOCAL_BUILD_UPLOADED.getName());
		buildProductEntity.setErrorMessage(null);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as SD_CSEZ_BUILD_UPLOADED");
	}

	public void markSDPreBuildUploaded(BuildProductEntity buildProductEntity)
	{
		buildProductEntity.setStatus(BuildProductStatus.SD_PRE_BUILD_UPLOADED.getName());
		buildProductEntity.setErrorMessage(null);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as SD_PRE_BUILD_UPLOADED");
	}

	public void markSDCSEZBuildUploadFailed(BuildProductEntity buildProductEntity, String errorMessage)
	{
		buildProductEntity.setStatus(BuildProductStatus.SD_CSEZ_BUILD_UPLOAD_FAILED.getName());
		buildProductEntity.setErrorMessage(null);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as SD_CSEZ_BUILD_UPLOAD_FAILED");
	}

	public void markSDLocalBuildUploadFailed(BuildProductEntity buildProductEntity, String errorMessage)
	{
		buildProductEntity.setStatus(BuildProductStatus.SD_LOCAL_BUILD_UPLOAD_FAILED.getName());
		buildProductEntity.setErrorMessage(errorMessage);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as SD_LOCAL_BUILD_UPLOAD_FAILED");
	}

	public void markSDPreBuildUploadFailed(BuildProductEntity buildProductEntity, String errorMessage)
	{
		buildProductEntity.setStatus(BuildProductStatus.SD_PRE_BUILD_UPLOAD_FAILED.getName());
		buildProductEntity.setErrorMessage(errorMessage);
		buildProductEntity.updateUpdatedTime();
		buildProductRepository.save(buildProductEntity);
		LOGGER.info("BuildProduct " + buildProductEntity.getId() + " for " + buildProductEntity.getProductName() + " marked as SD_PRE_BUILD_UPLOAD_FAILED");
	}

}
