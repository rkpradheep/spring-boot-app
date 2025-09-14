package com.server.zoho.workflow.steps;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.server.framework.common.AppProperties;
import com.server.framework.common.DateUtil;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.IntegService;
import com.server.zoho.entity.BuildMonitorEntity;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;

@Component
public class SDBuildUploadStep extends WorkflowStep
{
	@Autowired
	private BuildMonitorService buildMonitorService;

	@Autowired
	private BuildProductService buildProductService;

	@Autowired
	private RestTemplate restTemplate;

	private static final Logger LOGGER = Logger.getLogger(SDBuildUploadStep.class.getName());

	@Override public WorkflowEvent execute(WorkflowInstance instance)
	{

		try
		{
			@SuppressWarnings("unchecked")
			Map<String, Object> context = (Map<String, Object>) instance.getContext();
			Long monitorId = (Long) context.get("monitorId");

			BuildMonitorEntity monitor = buildMonitorService.getById(monitorId).orElse(null);

			assert monitor != null;
			monitor.setStatus("COMPLETED");
			buildMonitorService.save(monitor);

			String milestoneVersion = instance.getVariable("milestoneVersion");
			String productName = instance.getVariable("productName");

			LOGGER.info("Preparing SD Build Update for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion + ", productName: " + productName);


			String response = uploadBuild(productName, milestoneVersion, "CSEZ", "SEZ");
			LOGGER.info("SD Build Update Response CSEZ : " + response);

			boolean isCSEZBuildSuccessful =  new JSONObject(response).getString("code").equals("SUCCESS");
			String csezBuildMessage = new JSONObject(response).getString("message");

			response = uploadBuild(productName, milestoneVersion, "CT1", "CT");
			LOGGER.info("SD Build Update Response LOCAL : " + response);

			boolean isLocalBuildSuccessful =  new JSONObject(response).getString("code").equals("SUCCESS");
			String localBuildMessage = new JSONObject(response).getString("message");

			if(isCSEZBuildSuccessful && isLocalBuildSuccessful)
			{
				LOGGER.info("SD Build Update Successful for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion);
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_COMPLETED, Map.of("message", "All products completed"));
			}

			LOGGER.severe("SD Build Update Failed: " + "CSEZ Message : "  + csezBuildMessage + " | LOCAL Message : " + localBuildMessage);
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "SD Build Update Failed: " +  "CSEZ Message : "  + csezBuildMessage + " | LOCAL Message : " + localBuildMessage));

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error in SDBuildUploadStep execute", e);
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", e.getMessage()));
		}
	}

	private String uploadBuild(String productName, String milestoneVersion, String dc, String region)
	{
		JSONObject buildOptions = new JSONObject()
			.put("skip_continue", true)
			.put("iast_jar_needed", true);

		JSONArray notifyTo = new JSONArray().put("pradheep.rkd@zohocorp.com");

		String comment = DateUtil.getFormattedCurrentTime("'MASTER BUILD' dd'th' MMMM yyyy");
		JSONObject sdBuildUpdatePayload = new JSONObject()
			.put("data_center", dc)
			.put("region", region)
			.put("deployment_mode", "live")
			.put("build_stage", "production")
			.put("build_url", IntegService.getProductConfig(productName).getBuildUrl().replace("{0}", milestoneVersion))
			.put("is_grid_edited", false)
			.put("build_options", buildOptions)
			.put("notify_to", notifyTo)
			.put("comment", comment)
			.put("provision_type", "build_update");

		String sdBuildUpdateUrl = AppProperties.getProperty("zoho.sd.build.update.api.url");
		String apiKey = AppProperties.getProperty("zoho.sd.build.api.token");

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", apiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> requestEntity = new HttpEntity<>(sdBuildUpdatePayload.toString(), headers);

		return restTemplate.postForObject(sdBuildUpdateUrl, requestEntity, String.class);
	}
}
