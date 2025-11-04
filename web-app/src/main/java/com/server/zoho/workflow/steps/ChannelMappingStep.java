package com.server.zoho.workflow.steps;

import com.server.framework.common.CommonService;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.ZohoService;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.service.BuildProductService;
import com.server.zoho.service.MilestoneService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.zoho.workflow.model.BuildEventType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ChannelMappingStep extends WorkflowStep
{

	@Autowired
	private MilestoneService milestoneService;

	@Autowired
	private BuildProductService buildProductService;

	public ChannelMappingStep()
	{
		super();
		setName("Channel Mapping");
		setDescription("Maps the created milestone to the appropriate channel by calling the Zoho channel API");
		setType(StepType.ACTION);
		setTimeoutSeconds(120);
		setMaxRetries(0);
		setActionClass(this.getClass().getSimpleName());
	}

	@Override
	public WorkflowEvent execute(WorkflowInstance instance)
	{
		Map<String, Object> context = (Map<String, Object>) instance.getContext();
		Long productId = (Long) context.get("productId");
		Long buildId = Long.parseLong(instance.getVariable("buildId"));
		String milestoneVersion = instance.getVariable("milestoneVersion");

		Optional<BuildProductEntity> productOpt = buildProductService.getById(productId);

		try
		{
			BuildProductEntity product = productOpt.get();
			MilestoneService.ChannelMappingResult result = milestoneService.mapMilestoneToChannel(buildId, milestoneVersion, product.getProductName());

			if(result.isSuccess() && result.getChannelUrl() != null)
			{
				//ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + product.getProductName() + " ]* Channel Mapped");
				buildProductService.markChannelMapped(product, result.getChannelUrl());
				return new WorkflowEvent(BuildEventType.NEXT_PRODUCT, Map.of("channelUrl", result.getChannelUrl()));
			}
			else
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + product.getProductName() + " ]* Channel Mapping Failed");
				return new WorkflowEvent(BuildEventType.CHANNEL_MAPPING_FAILED, Map.of("error", result.getMessage()));
			}

		}
		catch(Exception e)
		{
			buildProductService.markChannelMappingFailed(productOpt.orElse(null), e.getMessage());
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productOpt.get().getProductName() + " ]* Channel Mapping Failed");
			return new WorkflowEvent(BuildEventType.CHANNEL_MAPPING_FAILED, Map.of("error", "Channel mapping failed: " + e.getMessage()));
		}
	}
}
