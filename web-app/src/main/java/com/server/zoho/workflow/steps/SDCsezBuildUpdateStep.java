package com.server.zoho.workflow.steps;

import io.micrometer.common.util.StringUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.server.framework.common.AppContextHolder;
import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.job.TaskEnum;
import com.server.framework.service.JobService;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.zoho.IntegService;
import com.server.zoho.ZohoService;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.zoho.workflow.model.BuildEventType;

@Component
public class SDCsezBuildUpdateStep extends WorkflowStep
{
	@Autowired
	private BuildMonitorService buildMonitorService;

	@Autowired
	private BuildProductService buildProductService;

	@Autowired
	private RestTemplate restTemplate;

	private static final Logger LOGGER = Logger.getLogger(SDCsezBuildUpdateStep.class.getName());

	public SDCsezBuildUpdateStep() {
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
		Boolean isBugFixBuild = (Boolean) context.get("isBugFixBuild");

		String milestoneVersion = instance.getVariable("milestoneVersion");
		String productName = instance.getVariable("productName");


		try
		{
			LOGGER.info("Preparing SD Build Update for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion + ", productName: " + productName);

			String commentMessageFormat = Boolean.TRUE.equals(isBugFixBuild) ? "'Master Bugfix Build' dd MMMM yyyy" : "'Master Build' dd MMMM yyyy";
			String comments = DateUtil.getFormattedCurrentTime(commentMessageFormat).toUpperCase();
			String response = ZohoService.uploadBuild(productName, milestoneVersion, "CSEZ", "SEZ", comments);
			LOGGER.info("SD Build Update Response CSEZ : " + response);

			JSONObject responseJSON = new JSONObject(response);
			boolean isCSEZBuildSuccessful = responseJSON.getString("code").equals("SUCCESS");
			String csezBuildMessage = responseJSON.getString("message");

			String buildId = responseJSON.getJSONArray("details").getJSONObject(0).get("build_id") + "";

			if(isCSEZBuildSuccessful)
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build update initiated for CSEZ ( " + milestoneVersion + " )");

				JSONObject data = new JSONObject()
					.put("message_id", context.get("messageID"))
					.put("gitlab_issue_id", context.get("gitlabIssueID"))
					.put("server_repo_name", context.get("serverProductName"))
					.put("monitor_id", monitorId + "")
					.put("build_id", buildId);

				AppContextHolder.getBean(JobService.class).scheduleJob(TaskEnum.SD_STATUS_POLL_TASK.getTaskName(), data.toString(), DateUtil.ONE_MINUTE_IN_MILLISECOND);

				buildProductService.getById(productId).ifPresent(buildProductService::markSDCSEZBuildUpdateInitiated);
				LOGGER.info("SD Build Update Successful for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion);
				return new WorkflowEvent(BuildEventType.SD_LOCAL_BUILD_UPDATE, Map.of("message", "SD csez build upload completed"));
			}
			else
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build update to CSEZ Failed ( " + milestoneVersion + " )");

				String buildOwnerEmail = IntegService.getTodayBuildOwnerEmail();
				if(StringUtils.isNotEmpty(buildOwnerEmail))
				{
					ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build Owner {@" + buildOwnerEmail + "} , Please take it from here.");
				}

				LOGGER.severe("SD Build Update Failed: " + "CSEZ Message : " + csezBuildMessage);
				buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDCSEZBuildUpdateFailed(buildProductEntity, csezBuildMessage));
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "SD Build Update Failed: " + "CSEZ Message : " + csezBuildMessage));
			}
		}
		catch(Exception e)
		{
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build update to CSEZ Failed ( " + milestoneVersion + " )");

			String buildOwnerEmail = IntegService.getTodayBuildOwnerEmail();
			if(StringUtils.isNotEmpty(buildOwnerEmail))
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build Owner {@" + buildOwnerEmail + "} , Please take it from here.");
			}

			LOGGER.log(Level.SEVERE, "Error in SDBuildUploadStep execute", e);
			buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDCSEZBuildUpdateFailed(buildProductEntity, e.getMessage()));
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", e.getMessage()));
		}
	}

}
