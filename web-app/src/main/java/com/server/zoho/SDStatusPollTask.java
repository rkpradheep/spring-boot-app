package com.server.zoho;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.entity.JobEntity;
import com.server.framework.job.Task;
import com.server.framework.service.JobService;

@Service
public class SDStatusPollTask implements Task
{
	@Autowired
	private JobService jobService;

	private static final Logger LOGGER = Logger.getLogger(SDStatusPollTask.class.getName());

	boolean canAddJobAgain = false;

	@Override public void run(long jobID) throws Exception
	{
		LOGGER.info("Executing SDStatusPollTask for job ID: " + jobID);

		Optional<JobEntity> jobEntityOptional = jobService.getJob(jobID);
		if(jobEntityOptional.isEmpty())
		{
			LOGGER.log(Level.SEVERE, "Job not found for ID: " + jobID);
			return;
		}

		JobEntity jobEntity = jobEntityOptional.get();
		String data = jobEntity.getData();
		JSONObject jsonObject = new JSONObject(data);
		String messageID = jsonObject.getString("message_id");
		String gitlabIssueID = jsonObject.getString("gitlab_issue_id");
		String serverRepoName = jsonObject.getString("server_repo_name");
		String buildID = jsonObject.getString("build_id");

		JSONObject response = ZohoService.getSDBuildStatus(buildID);

		LOGGER.info("Response from SD : " + response);

		String overallStatus = response.getString("overall_status");

		canAddJobAgain = false;
		String message;
		String url = response.getString("url");
		String region = response.getString("region");
		String regionName = region.equals("CT") ? "LOCAL" : "CSEZ";
		String buildUrlMessage = "\n\nBuild URL : " + url;
		boolean isSuccess = false;
		if(overallStatus.equalsIgnoreCase("Failed"))
		{
			message = "Build update failed in " + regionName + buildUrlMessage;
		}
		else if(overallStatus.equalsIgnoreCase("Completed"))
		{
			message = "Build deployed successfully in " + regionName + buildUrlMessage;
			isSuccess = true;
		}
		else
		{
			LOGGER.log(Level.INFO, "Build is not completed yet. Status received: {0}", overallStatus);
			canAddJobAgain = true;
			return;
		}

		ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), messageID, serverRepoName, gitlabIssueID, "MASTER BUILD", message + "\n\nPlease start the testing {@participants}");
		String buildOwnerEmail = IntegService.getTodayBuildOwnerEmail();
		if(StringUtils.isNotEmpty(buildOwnerEmail) && regionName.equals("LOCAL"))
		{
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), messageID, serverRepoName, gitlabIssueID, "MASTER BUILD","Build Owner {@" + buildOwnerEmail + "} , Please take it from here.");
		}
		if(isSuccess)
		{
			JSONObject reference = new JSONObject()
				.put("type", "button")
				.put("object", new JSONObject()
					.put("label", "Move To Pre")
					.put("action", new JSONObject()
						.put("type", "invoke.function")
						.put("data", new JSONObject().put("name", "payoutuploadtopre")))
					.put("arguments", new JSONObject()
						.put("key", "movetopre")
						.put("value", jsonObject.optString("monitor_id")))
					.put("type", "+")
				);

			JSONObject references = new JSONObject().put("1", reference);

			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), messageID, serverRepoName, gitlabIssueID, "MASTER BUILD", "[Move To Pre]($1)", references);
		}
	}

	@Override public boolean canAddJobAgain()
	{
		return canAddJobAgain;
	}

	@Override public long getDelayBeforeAddJobAgain()
	{
		return DateUtil.ONE_SECOND_IN_MILLISECOND * 30;
	}
}
