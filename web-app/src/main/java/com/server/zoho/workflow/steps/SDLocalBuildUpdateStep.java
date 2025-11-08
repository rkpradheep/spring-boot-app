package com.server.zoho.workflow.steps;

import io.micrometer.common.util.StringUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.framework.common.AppContextHolder;
import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.job.TaskEnum;
import com.server.framework.service.JobService;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.zoho.IntegService;
import com.server.zoho.ZohoService;
import com.server.zoho.entity.BuildMonitorEntity;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;

@Component
public class SDLocalBuildUpdateStep extends WorkflowStep
{
	@Autowired
	private BuildMonitorService buildMonitorService;

	@Autowired
	private BuildProductService buildProductService;

	private static final Logger LOGGER = Logger.getLogger(SDLocalBuildUpdateStep.class.getName());

	public SDLocalBuildUpdateStep() {
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
		String milestoneVersion = instance.getVariable("milestoneVersion");
		String productName = instance.getVariable("productName");

		try
		{
			LOGGER.info("Preparing SD Build Update for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion + ", productName: " + productName);

			String response = ZohoService.uploadBuild(productName, milestoneVersion, "CT1", "CT");
			LOGGER.info("SD Build Update Response LOCAL : " + response);

			JSONObject responseJSON = new JSONObject(response);
			boolean isLocalBuildSuccessful = responseJSON.getString("code").equals("SUCCESS");
			String localBuildMessage = responseJSON.getString("message");

			String buildId = responseJSON.getJSONArray("details").getJSONObject(0).getString("build_id");

			if(isLocalBuildSuccessful)
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build update initiated for LOCAL ( " + milestoneVersion + " )");

				JSONObject data = new JSONObject()
					.put("message_id", context.get("messageID"))
					.put("gitlab_issue_id", context.get("gitlabIssueID"))
					.put("server_repo_name", context.get("serverProductName"))
					.put("build_id", buildId);

				AppContextHolder.getBean(JobService.class).scheduleJob(TaskEnum.SD_STATUS_POLL_TASK.getTaskName(), data.toString(), DateUtil.ONE_MINUTE_IN_MILLISECOND);


				String buildOwnerEmail = IntegService.getTodayBuildOwnerEmail();
				if(StringUtils.isNotEmpty(buildOwnerEmail))
				{
					ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build Owner {@" + buildOwnerEmail + "} , Please take it from here.");
				}
				buildProductService.getById(productId).ifPresent(buildProductService::markSDLocalBuildUpdateInitiated);

				BuildMonitorEntity monitor = buildMonitorService.getById(monitorId).orElse(null);
				assert monitor != null;
				buildMonitorService.markAsCompleted(monitor);

				LOGGER.info("SD Build Update Successful for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion);
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_COMPLETED, Map.of("message", "SD local build upload completed"));
			}
			else
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build upload to CSEZ Failed ( " + milestoneVersion + " )");
				LOGGER.severe("SD Build Update Failed: " + "LOCAL Message : " + localBuildMessage);
				buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDLocalBuildUpdateFailed(buildProductEntity, localBuildMessage));
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "SD Build Update Failed: " + "Local Message : " + localBuildMessage));
			}
		}
		catch(Exception e)
		{
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build upload to LOCAL Failed ( " + milestoneVersion + " )");
			LOGGER.log(Level.SEVERE, "Error in SDBuildUploadStep execute", e);
			buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDLocalBuildUpdateFailed(buildProductEntity, e.getMessage()));
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", e.getMessage()));
		}
	}
}
