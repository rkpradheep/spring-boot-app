package com.server.zoho.workflow.steps;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.framework.common.AppContextHolder;
import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.job.TaskEnum;
import com.server.framework.service.ConfigurationService;
import com.server.framework.service.JobService;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.model.WorkflowStatus;
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

	public SDLocalBuildUpdateStep()
	{
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
		Boolean isBugFixBuild = (Boolean) context.get("isBugFixBuild");
		String milestoneVersion = instance.getVariable("milestoneVersion");
		String productName = instance.getVariable("productName");
		String serverRepoName = (String) context.get("serverProductName");
		Boolean isMigrationRequired = (Boolean) context.get("isMigrationRequired");
		Boolean isInvokedFromResumeFlow = (Boolean) context.get("isInvokedFromResumeFlow");

		try
		{

			context.put("isInvokedFromResumeFlow", false);
			if(Boolean.TRUE.equals(isMigrationRequired) && !Boolean.TRUE.equals(isInvokedFromResumeFlow))
			{
				context.put("buildStage", "CT");

				LOGGER.info("Migration required. Suspending workflow at SD Local Build Update for monitorId: " + monitorId);
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Workflow suspended for migration before SD LOCAL build update. Resume the workflow after migration is completed.");

				String initiateMigration = "[Initiate Meta Migration]($1)";

				JSONObject reference = new JSONObject()
					.put("type", "button")
					.put("object", new JSONObject()
						.put("label", "Initiate Migration")
						.put("action", new JSONObject()
							.put("type", "invoke.function")
							.put("data", new JSONObject().put("name", "initiatemetamigration")))
						.put("arguments", new JSONObject()
							.put("key", "initiateamigration")
							.put("value", monitorId + "")
							.put("type", "+")
						));

				JSONObject references = new JSONObject().put("1", reference);
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), (String) context.get("messageID"), null, null,  "MASTER BUILD", initiateMigration, references);



				String resumeWorkflow = "[Resume Workflow]($1)";

				 reference = new JSONObject()
					.put("type", "button")
					.put("object", new JSONObject()
						.put("label", "Resume Workflow")
						.put("action", new JSONObject()
							.put("type", "invoke.function")
							.put("data", new JSONObject().put("name", "resumebuildworkflow")))
						.put("arguments", new JSONObject()
							.put("key", "resumeworkflow")
							.put("value", monitorId + "")
							.put("type", "+")
						));

				 references = new JSONObject().put("1", reference);
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), (String) context.get("messageID"), null, null,  "MASTER BUILD", resumeWorkflow, references);

				instance.setStatus(WorkflowStatus.SUSPENDED);
				instance.setLastUpdateTime(System.currentTimeMillis());
				return null;
			}

			try
			{
				if(!StringUtils.equals(serverRepoName, "tpap_server"))
				{
					TimeUnit.MINUTES.sleep(1);
				}
			}
			catch(InterruptedException e)
			{
				LOGGER.log(Level.SEVERE, "Exception occurred", e);
			}

			LOGGER.info("Preparing SD Build Update for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion + ", productName: " + productName);

			String commentMessageFormat = Boolean.TRUE.equals(isBugFixBuild) ? "'Master Bugfix Build' dd MMMM yyyy" : "'Master Build' dd MMMM yyyy";
			String comments = DateUtil.getFormattedCurrentTime(commentMessageFormat).toUpperCase();

			String response = ZohoService.uploadBuild(productName, milestoneVersion, "CT1", "CT", comments);
			LOGGER.info("SD Build Update Response LOCAL : " + response);

			JSONObject responseJSON = new JSONObject(response);
			boolean isLocalBuildSuccessful = responseJSON.optString("code", "").equals("SUCCESS");
			String localBuildMessage = responseJSON.getString("message");

			if(isLocalBuildSuccessful)
			{
				AppContextHolder.getBean(ConfigurationService.class).setValue((String) context.get("masterBuildKey"), monitorId.toString(), DateUtil.getCurrentTimeInMillis() + DateUtil.ONE_DAY_IN_MILLISECOND);
				String initiatorMessage = "\n\nInitiated By : SCHEDULER";
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build update initiated successfully for LOCAL ( " + milestoneVersion + " )" + initiatorMessage);

				String buildId = responseJSON.getJSONArray("details").getJSONObject(0).get("build_id") + "";

				JSONObject data = new JSONObject()
					.put("message_id", context.get("messageID"))
					.put("gitlab_issue_id", context.get("gitlabIssueID"))
					.put("server_repo_name", serverRepoName)
					.put("monitor_id", monitorId + "")
					.put("product_id", productId + "")
					.put("build_id", buildId);

				AppContextHolder.getBean(JobService.class).scheduleJob(TaskEnum.SD_STATUS_POLL_TASK.getTaskName(), data.toString(), DateUtil.ONE_MINUTE_IN_MILLISECOND);

				buildProductService.getById(productId).ifPresent(buildProductService::markSDLocalBuildUpdateInitiated);

				BuildMonitorEntity monitor = buildMonitorService.getById(monitorId).orElse(null);
				assert monitor != null;
				buildMonitorService.markAsCompleted(monitor);

				LOGGER.info("SD Build Update Successful for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion);
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_COMPLETED, Map.of("message", "SD local build upload completed"));
			}
			else
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build update to LOCAL Failed ( " + milestoneVersion + " )" + " : " + localBuildMessage);

				String buildOwnerEmail = IntegService.getTodayBuildOwnerEmail(serverRepoName);
				if(StringUtils.isNotEmpty(buildOwnerEmail))
				{
					ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build Owner {@" + buildOwnerEmail + "} , Please take it from here.", false);
				}

				LOGGER.severe("SD Build Update Failed: " + "LOCAL Message : " + localBuildMessage);
				buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDLocalBuildUpdateFailed(buildProductEntity, localBuildMessage));

				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "SD Build Update Failed: " + "Local Message : " + localBuildMessage));

			}
		}
		catch(Exception e)
		{
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build update to LOCAL Failed ( " + milestoneVersion + " )");

			String buildOwnerEmail = IntegService.getTodayBuildOwnerEmail(serverRepoName);
			if(StringUtils.isNotEmpty(buildOwnerEmail))
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build Owner {@" + buildOwnerEmail + "} , Please take it from here.", false);
			}

			LOGGER.log(Level.SEVERE, "Error in SDBuildUploadStep execute", e);
			buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDLocalBuildUpdateFailed(buildProductEntity, e.getMessage()));
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", e.getMessage()));
		}
	}
}
