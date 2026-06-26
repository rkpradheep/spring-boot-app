package com.server.zoho;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.entity.JobEntity;
import com.server.framework.job.Task;
import com.server.framework.service.JobService;
import com.server.framework.service.WorkflowService;
import com.server.framework.workflow.WorkflowEngine;
import com.server.zoho.service.BuildProductService;

@Service @Scope("prototype") public class SDMigrationStatusPollTask implements Task
{
	@Autowired private JobService jobService;

	@Autowired private BuildProductService buildProductService;

	@Autowired private WorkflowService workflowService;

	@Autowired private WorkflowEngine workflowEngine;

	private static final Logger LOGGER = Logger.getLogger(SDMigrationStatusPollTask.class.getName());

	boolean canAddJobAgain = false;

	@Override public void run(long jobID) throws Exception
	{
		LOGGER.info("Executing SDMigrationStatusPollTask for job ID: " + jobID);

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

		JSONObject response = ZohoService.getSDMigrationBuildStatus(serverRepoName, buildID);

		LOGGER.info("Response from SD : " + response);

		String overallStatus = response.getString("overall_status");

		canAddJobAgain = false;

		boolean isKilled = false;

		if(StringUtils.equalsIgnoreCase(overallStatus, "killed"))
		{
			isKilled = true;
			LOGGER.log(Level.INFO, "Migration process was killed for build ID: {0}", buildID);
		}
		String message;
		String url = response.getString("url");
		String region = response.getString("region");
		String regionName = region.equals("CT") ? "LOCAL" : "CSEZ";
		regionName = region.equals("IN") ? response.getString("build_stage").equals("production") ? "PRODUCTION" : "PRE" : regionName;
		String buildUrlMessage = "\n\nBuild URL : " + url;

		if(overallStatus.equalsIgnoreCase("Failed") || isKilled)
		{
			message = "Migration failed in " + regionName + buildUrlMessage + (isKilled ? "\n\nReason: Migration was aborted" : "");
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), messageID, serverRepoName, gitlabIssueID, "MASTER BUILD", message);
		}
		else if(overallStatus.equalsIgnoreCase("Completed"))
		{
			message = "Migration completed successfully in " + regionName + buildUrlMessage;
			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), messageID, serverRepoName, gitlabIssueID, "MASTER BUILD", message);
		}
		else
		{
			LOGGER.log(Level.INFO, "Migration is not completed yet. Status received: {0}", overallStatus);
			canAddJobAgain = true;
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
