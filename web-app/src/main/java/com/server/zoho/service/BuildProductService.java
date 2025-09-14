package com.server.zoho.service;

import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.repository.BuildProductRepository;

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
		buildProductEntity.setStatus("CHANNEL_MAPPED");
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

	public boolean areAllProductsCompleted(Long buildMonitorId)
	{
		List<BuildProductEntity> products = getProductsForMonitor(buildMonitorId);
		return products.stream().allMatch(p -> "CHANNEL_MAPPED".equals(p.getStatus()));
	}

	public boolean areAllProductsFailed(Long buildMonitorId)
	{
		List<BuildProductEntity> products = getProductsForMonitor(buildMonitorId);
		return products.stream().allMatch(p ->
			"FAILED".equals(p.getStatus()) ||
				"MILESTONE_FAILED".equals(p.getStatus()) ||
				"CHANNEL_FAILED".equals(p.getStatus())
		);
	}

	public BuildMonitorStats getBuildStats(Long buildMonitorId)
	{
		long total = buildProductRepository.countByBuildMonitorId(buildMonitorId);
		long completed = buildProductRepository.countCompletedProducts(buildMonitorId);
		long pending = buildProductRepository.countByBuildMonitorIdAndStatus(buildMonitorId, "PENDING");
		long inProgress = buildProductRepository.countByBuildMonitorIdAndStatus(buildMonitorId, "STARTED");
		long success = buildProductRepository.countByBuildMonitorIdAndStatus(buildMonitorId, "SUCCESS");
		long failed = buildProductRepository.countByBuildMonitorIdAndStatus(buildMonitorId, "FAILED");

		return new BuildMonitorStats(total, completed, pending, inProgress, success, failed);
	}

	public Optional<BuildProductEntity> findByBuildId(Long buildId)
	{
		return buildProductRepository.findByBuildId(buildId);
	}

	public List<BuildProductEntity> cleanupStaleBuilds()
	{
		long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L); // 1 hour ago
		List<BuildProductEntity> staleProducts = buildProductRepository.findStaleInProgressProducts(oneHourAgo);

		for(BuildProductEntity product : staleProducts)
		{
			markBuildFailed(product, "Build became stale (no updates for 1 hour)");
		}

		if(!staleProducts.isEmpty())
		{
			LOGGER.warning("Cleaned up " + staleProducts.size() + " stale builds");
		}

		return staleProducts;
	}

	public void deleteBuildMonitor(Long buildMonitorId)
	{
		buildProductRepository.deleteByBuildMonitorId(buildMonitorId);
		LOGGER.info("Deleted all BuildProduct entries for BuildMonitor " + buildMonitorId);
	}

	public static class BuildMonitorStats
	{
		private final long total;
		private final long completed;
		private final long pending;
		private final long inProgress;
		private final long success;
		private final long failed;

		public BuildMonitorStats(long total, long completed, long pending, long inProgress, long success, long failed)
		{
			this.total = total;
			this.completed = completed;
			this.pending = pending;
			this.inProgress = inProgress;
			this.success = success;
			this.failed = failed;
		}

		public long getTotal()
		{
			return total;
		}

		public long getCompleted()
		{
			return completed;
		}

		public long getPending()
		{
			return pending;
		}

		public long getInProgress()
		{
			return inProgress;
		}

		public long getSuccess()
		{
			return success;
		}

		public long getFailed()
		{
			return failed;
		}

		public double getCompletionPercentage()
		{
			return total > 0 ? (double) completed / total * 100 : 0.0;
		}

		public boolean isCompleted()
		{
			return total > 0 && total == completed;
		}
	}
}
