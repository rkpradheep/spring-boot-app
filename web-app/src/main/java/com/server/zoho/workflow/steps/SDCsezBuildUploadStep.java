package com.server.zoho.workflow.steps;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.server.framework.common.CommonService;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.ZohoService;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.zoho.workflow.model.BuildEventType;

@Component
public class SDCsezBuildUploadStep extends WorkflowStep
{
	@Autowired
	private BuildMonitorService buildMonitorService;

	@Autowired
	private BuildProductService buildProductService;

	@Autowired
	private RestTemplate restTemplate;

	private static final Logger LOGGER = Logger.getLogger(SDCsezBuildUploadStep.class.getName());

	public SDCsezBuildUploadStep() {
		super();
		setName("SD CSEZ Build Upload");
		setDescription("Uploads the build to CSEZ");
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

		String milestoneVersion = instance.getVariable("milestoneVersion");
		String productName = instance.getVariable("productName");


		try
		{
			LOGGER.info("Preparing SD Build Update for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion + ", productName: " + productName);

			String response = ZohoService.uploadBuild(productName, milestoneVersion, "CSEZ", "SEZ");
			LOGGER.info("SD Build Update Response CSEZ : " + response);

			boolean isCSEZBuildSuccessful = new JSONObject(response).getString("code").equals("SUCCESS");
			String csezBuildMessage = new JSONObject(response).getString("message");

			if(isCSEZBuildSuccessful)
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build upload initiated for CSEZ ( " + milestoneVersion + " )");

				buildProductService.getById(productId).ifPresent(buildProductService::markSDCSEZBuildUploaded);
				LOGGER.info("SD Build Update Successful for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion);
				return new WorkflowEvent(BuildEventType.SD_LOCAL_BUILD_UPLOAD, Map.of("message", "SD csez build upload completed"));
			}
			else
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build upload to CSEZ Failed ( " + milestoneVersion + " )");
				LOGGER.severe("SD Build Update Failed: " + "CSEZ Message : " + csezBuildMessage);
				buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDCSEZBuildUploadFailed(buildProductEntity, csezBuildMessage));
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "SD Build Update Failed: " + "CSEZ Message : " + csezBuildMessage));
			}
		}
		catch(Exception e)
		{
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build upload to CSEZ Failed ( " + milestoneVersion + " )");
			LOGGER.log(Level.SEVERE, "Error in SDBuildUploadStep execute", e);
			buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDCSEZBuildUploadFailed(buildProductEntity, e.getMessage()));
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", e.getMessage()));
		}
	}

}
