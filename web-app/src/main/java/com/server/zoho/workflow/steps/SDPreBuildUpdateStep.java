package com.server.zoho.workflow.steps;

import io.micrometer.common.util.StringUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.framework.common.AppProperties;
import com.server.framework.common.CommonService;
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
public class SDPreBuildUpdateStep extends WorkflowStep
{
	@Autowired
	private BuildMonitorService buildMonitorService;

	@Autowired
	private BuildProductService buildProductService;

	private static final Logger LOGGER = Logger.getLogger(SDPreBuildUpdateStep.class.getName());

	public SDPreBuildUpdateStep() {
		super();
		setName("SD PRE Build Upload");
		setDescription("Uploads the build to PRE");
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
		boolean isPatchBuildUpdate = Boolean.TRUE.equals(context.get("isPatchBuild"));
		String buildURL = isPatchBuildUpdate ? instance.getVariable("buildUrl") : null;
		String milestoneVersion = instance.getVariable("milestoneVersion");
		String productName = isPatchBuildUpdate ? (String) context.get("productName") : instance.getVariable("productName");
		Boolean isMigrationRequired = (Boolean) context.get("isMigrationRequired");
		Boolean isInvokedFromResumeFlow = (Boolean) context.get("isInvokedFromResumeFlow");

		try
		{
			LOGGER.info("Preparing SD Build Update for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion + ", productName: " + productName);

			context.put("isInvokedFromResumeFlow", false);
			if(Boolean.TRUE.equals(isMigrationRequired) && !Boolean.TRUE.equals(isInvokedFromResumeFlow))
			{
				LOGGER.info("Migration required. Suspending workflow at SD CSEZ Build Update for monitorId: " + monitorId);
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Workflow suspended for migration before SD PRE build update. Resume the workflow after migration is completed.");

				String resumeWorkflow = "[Resume Workflow]($1)";

				JSONObject reference = new JSONObject()
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

				JSONObject references = new JSONObject().put("1", reference);
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), (String) context.get("messageID"), null, null,  "MASTER BUILD", resumeWorkflow, references);

				instance.setStatus(WorkflowStatus.SUSPENDED);
				instance.setLastUpdateTime(System.currentTimeMillis());
				return null;
			}

			String response = ZohoService.uploadBuild(productName, milestoneVersion, AppProperties.getProperty("zoho.in.dc.main"), "IN", "pre", isPatchBuildUpdate, buildURL);
			LOGGER.info("SD Build Update Response PRE : " + response);

			boolean isPreBuildSuccessful = new JSONObject(response).optString("code", "").equals("SUCCESS");
			String preBuildMessage = new JSONObject(response).getString("message");

			if(isPreBuildSuccessful)
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build update initiated for PRE ( " + milestoneVersion + " )" + " : " + preBuildMessage);

				String buildOwnerEmail = IntegService.getTodayBuildOwnerEmail((String) context.get("serverProductName"));
				if(StringUtils.isNotEmpty(buildOwnerEmail))
				{
					ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build Owner {@" + buildOwnerEmail + "} , Please take it from here.", false);
				}
				buildProductService.getById(productId).ifPresent(buildProductService::markSDPreBuildUpdateFailed);

				BuildMonitorEntity monitor = buildMonitorService.getById(monitorId).orElse(null);
				assert monitor != null;
				buildMonitorService.markAsCompleted(monitor);

				LOGGER.info("SD Build Update Successful for monitorId: " + monitorId + ", milestoneVersion: " + milestoneVersion);
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_COMPLETED, Map.of("message", "SD pre build upload completed"));
			}
			else
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build upload to CSEZ Failed ( " + milestoneVersion + " )");
				LOGGER.severe("SD Build Update Failed: " + "PRE Message : " + preBuildMessage);
				buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDPreBuildUploadFailed(buildProductEntity, preBuildMessage));
				return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", "SD Build Update Failed: " + "Pre Message : " + preBuildMessage));
			}
		}
		catch(Exception e)
		{
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build upload to PRE Failed ( " + milestoneVersion + " )");
			LOGGER.log(Level.SEVERE, "Error in SDBuildUploadStep execute", e);
			buildProductService.getById(productId).ifPresent(buildProductEntity -> buildProductService.markSDPreBuildUploadFailed(buildProductEntity, e.getMessage()));
			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", e.getMessage()));
		}
	}
}
