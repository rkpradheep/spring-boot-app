package com.server.zoho.workflow.steps;

import com.server.framework.common.CommonService;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.BuildResponse;
import com.server.zoho.IntegService;
import com.server.zoho.ZohoService;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.service.BuildProductService;
import com.server.zoho.service.MilestoneService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.zoho.workflow.model.BuildEventType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class MilestoneCreationStep extends WorkflowStep
{
	private static final Logger LOGGER = Logger.getLogger(MilestoneCreationStep.class.getName());
	@Autowired
	private MilestoneService milestoneService;

	@Autowired
	private BuildProductService buildProductService;

	@Autowired
	private IntegService integService;

	public MilestoneCreationStep()
	{
		super();
		setName("Milestone Creation");
		setDescription("Creates a milestone for the completed build by calling the Zoho milestone API");
		setType(StepType.ACTION);
		setTimeoutSeconds(120);
		setMaxRetries(0);
		setActionClass(this.getClass().getSimpleName());
	}

	@Override
	public WorkflowEvent execute(WorkflowInstance instance)
	{
		@SuppressWarnings("unchecked")
		Map<String, Object> context = (Map<String, Object>) instance.getContext();
		Long productId = (Long) context.get("productId");
		Long buildId = Long.parseLong(instance.getVariable("buildId"));

		Optional<BuildProductEntity> productOpt = buildProductService.getById(productId);

		try
		{
			BuildResponse buildResponse = integService.checkBuildStatus(buildId);
			String releaseVersion = buildResponse.getReleaseVersion();
			BuildProductEntity product = productOpt.get();
			if(StringUtils.isEmpty(releaseVersion))
			{
				MilestoneService.MilestoneResult result = milestoneService.moveBuildToMilestone(buildId, product.getProductName());
				if(result.isSuccess() && result.getMilestoneVersion() != null)
				{
					ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + product.getProductName() + " ]* COMPLETED\n\nMilestone Version: " + result.getMilestoneVersion());
					//ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productOpt.get().getProductName() + " ]* Milestone created ( " + result.getMilestoneVersion() + " )");
					releaseVersion = result.getMilestoneVersion();
				}
				else
				{
					ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productOpt.get().getProductName() + " ]* Milestone Failed");
					return new WorkflowEvent(BuildEventType.MILESTONE_FAILED, Map.of("error", result.getMessage()));
				}
			}
			else
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productOpt.get().getProductName() + " ]* Milestone Already Available( " + releaseVersion + " )");
				LOGGER.info("Milestone already available. Using release version from build response: " + releaseVersion);
			}

			buildProductService.markMilestoneCreated(product, releaseVersion);
			instance.setVariable("milestoneVersion", releaseVersion);
			instance.setVariable("productName", product.getProductName());
			boolean isServerRepo = IntegService.getProductConfig(product.getProductName()).isServerRepo();

			return new WorkflowEvent(!isServerRepo ? BuildEventType.CHANNEL_MAPPING : BuildEventType.SD_CSEZ_BUILD_UPLOAD, Map.of("milestoneVersion", releaseVersion));

		}
		catch(Exception e)
		{
			buildProductService.markMilestoneFailed(productOpt.orElse(null), e.getMessage());
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productOpt.get().getProductName() + " ]* Milestone Failed");
			return new WorkflowEvent(BuildEventType.MILESTONE_FAILED, Map.of("error", "Milestone creation failed: " + e.getMessage()));
		}
	}
}
