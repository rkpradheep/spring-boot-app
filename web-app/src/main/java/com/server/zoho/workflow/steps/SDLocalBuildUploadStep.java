package com.server.zoho.workflow.steps;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.zoho.ZohoController;
import com.server.zoho.entity.BuildMonitorEntity;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;

@Component
public class SDLocalBuildUploadStep extends WorkflowStep
{
	@Autowired
	private BuildMonitorService buildMonitorService;

	@Autowired
	private BuildProductService buildProductService;

	private static final Logger LOGGER = Logger.getLogger(SDLocalBuildUploadStep.class.getName());

	public SDLocalBuildUploadStep() {
		super();
		setName("SD LOCAL Build Upload");
		setDescription("Uploads the build to LOCAL");
		setType(StepType.ACTION);
		setTimeoutSeconds(10);
		setMaxRetries(0);
		setActionClass(this.getClass().getSimpleName());
	}

	@Override public WorkflowEvent execute(WorkflowInstance instance)
	{
		@SuppressWarnings("unchecked")
		Map<String, Object> context = (Map<String, Object>) instance.getContext();
		Long monitorId = (Long) context.get("monitorId");
		Long productId = (Long) context.get("productId");

		try
		{
			String milestoneVersion = instance.getVariable("milestoneVersion");
			String productName = instance.getVariable("productName");

			LOGGER.info("Preparing SD Build Update for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion + ", productName: " + productName);

			String response = ZohoController.uploadBuild(productName, milestoneVersion, "CT1", "CT");
			LOGGER.info("SD Build Update Response LOCAL : " + response);

			boolean isLocalBuildSuccessful = new JSONObject(response).getString("code").equals("SUCCESS");
			String localBuildMessage = new JSONObject(response).getString("message");

			if(isLocalBuildSuccessful)
			{
				buildProductService.getById(productId).ifPresent(buildProductService::markSDLocalBuildUploaded);

				BuildMonitorEntity monitor = buildMonitorService.getById(monitorId).orElse(null);
				assert monitor != null;
				buildMonitorService.markAsCompleted(monitor);

				LOGGER.info("SD Build Update Successful for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion);
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_COMPLETED, Map.of("message", "SD local build upload completed"));
			}
			else
			{
				LOGGER.severe("SD Build Update Failed: " + "LOCAL Message : " + localBuildMessage);
				buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDLocalBuildUploadFailed(buildProductEntity, localBuildMessage));
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "SD Build Update Failed: " + "Local Message : " + localBuildMessage));
			}
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error in SDBuildUploadStep execute", e);
			buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDLocalBuildUploadFailed(buildProductEntity, e.getMessage()));
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", e.getMessage()));
		}
	}
}
