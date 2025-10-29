package com.server.zoho.workflow.steps;

import com.server.framework.common.CommonService;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.BuildResponse;
import com.server.zoho.IntegService;
import com.server.zoho.ZohoService;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.service.BuildProductService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.zoho.workflow.model.BuildEventType;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class BuildInitiationStep extends WorkflowStep
{

	@Autowired
	private IntegService integService;

	@Autowired
	private BuildProductService buildProductService;

	public BuildInitiationStep()
	{
		super();
		setName("Build Initiated");
		setDescription("Initiates a build for the specified product by calling the Zoho build API");
		setType(StepType.ACTION);
		setTimeoutSeconds(60);
		setMaxRetries(2);
		setActionClass(this.getClass().getSimpleName());
	}

	@Override
	public String getFailureEventName()
	{
		return BuildEventType.BUILD_FAILED.getValue();
	}

	@Override
	public WorkflowEvent execute(WorkflowInstance instance)
	{
		Map<String, Object> context = (Map<String, Object>) instance.getContext();
		Long productID = (Long) context.get("productId");

		String productName = buildProductService.getById(productID).map(BuildProductEntity::getProductName).orElse(null);
		if(StringUtils.isEmpty(productName))
		{
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "Product name not found in context"));
		}

		try
		{
			BuildResponse response = integService.initiateBuild(productName, Boolean.TRUE.equals(context.get("isPatchBuild")), (String)context.get("branchName"));

			if(response.isSuccess() && response.getBuildLogId() != null)
			{
				instance.setVariable("buildId", response.getBuildLogId());
				instance.setVariable("productId", response.getProductId());
				instance.setVariable("buildType", response.getBuildType());
				instance.setVariable("checkoutLabel", response.getCheckoutLabel());

				Long productId = (Long) context.get("productId");
				Optional<BuildProductEntity> productOpt = buildProductService.getById(productId);
				productOpt.ifPresent(product -> buildProductService.markBuildStarted(product,  response.getBuildLogId()));

				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productName + " ]* Build started");

				return new WorkflowEvent(BuildEventType.BUILD_STARTED, Map.of("buildId", response.getBuildLogId(), "message", response.getMessage()));
			}
			else
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productName + " ]* Build Failed");
				return new WorkflowEvent(BuildEventType.BUILD_FAILED, Map.of("error", response.getMessage()));
			}
		}
		catch(Exception e)
		{
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productName + " ]* Build Failed");
			return new WorkflowEvent(BuildEventType.BUILD_FAILED, Map.of("error", "Failed to start build: " + e.getMessage()));
		}
	}

}
