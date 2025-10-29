package com.server.zoho.workflow.steps;

import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.job.TaskEnum;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.BuildResponse;
import com.server.zoho.IntegService;
import com.server.zoho.ZohoService;
import com.server.zoho.entity.BuildProductEntity;
import com.server.zoho.service.BuildProductService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.zoho.workflow.model.BuildEventType;

import java.util.Map;
import java.util.logging.Logger;

@Component public class BuildInProgressStep extends WorkflowStep
{
	private static final Logger LOGGER = Logger.getLogger(BuildInProgressStep.class.getName());

	@Autowired private IntegService integService;

	@Autowired private BuildProductService buildProductService;

	@Autowired private JobService jobService;

	public BuildInProgressStep()
	{
		super();
		setName("Build In Progress");
		setDescription("Monitors the build status by polling the Zoho build API until completion");
		setType(StepType.ACTION);
		setTimeoutSeconds(300);
		setMaxRetries(0);
		setActionClass(this.getClass().getSimpleName());
	}

	@Override public WorkflowEvent execute(WorkflowInstance instance)
	{
		@SuppressWarnings("unchecked")
		Map<String, Object> context = (Map<String, Object>) instance.getContext();
		Long productId = (Long) context.get("productId");
		Long buildId = Long.parseLong(instance.getVariable("buildId"));

		if(productId == null)
		{
			return new WorkflowEvent(BuildEventType.BUILD_FAILED, Map.of("error", "Missing productId or buildId"));
		}

		try
		{
			BuildResponse response = integService.checkBuildStatus(buildId);
			String status = response.getMessage();

			String productName = buildProductService.getById(productId).map(BuildProductEntity::getProductName).orElse(null);

			if("BUILD_SUCCESS".equals(status))
			{
				instance.setVariable("buildDuration", response.getDuration());
				instance.setVariable("buildUrl", response.getUrl());

				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productName + " ]* Build Success");

				return new WorkflowEvent(BuildEventType.BUILD_COMPLETED, Map.of("message", "Build completed successfully", "buildLogId", response.getBuildLogId()));
			}
			else if("BUILD_FAILED".equals(status))
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "*[ " + productName + " ]* Build Failed");
				return new WorkflowEvent(BuildEventType.BUILD_FAILED, Map.of("error", response.getErrorMessage() != null ? response.getErrorMessage() : "Build failed"));
			}
			else
			{
				LOGGER.info("Build " + buildId + " is still in progress. ReferenceID : " + instance.getReferenceID());
				String data = new ObjectMapper().writeValueAsString(Map.of("instanceId", instance.getReferenceID()));
				jobService.scheduleJob(TaskEnum.WORKFLOW_TASK.getTaskName(), data, DateUtil.ONE_SECOND_IN_MILLISECOND * 30);
				return null;
			}
		}
		catch(Exception e)
		{
			return new WorkflowEvent(BuildEventType.BUILD_FAILED, Map.of("error", "Build status check failed: " + e.getMessage()));
		}
	}

}
