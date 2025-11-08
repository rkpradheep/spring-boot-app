package com.server.zoho;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		if(overallStatus.equals("In Progress"))
		{
			canAddJobAgain = true;
			return;
		}

		canAddJobAgain = false;
		String message;
		String url = response.getString("url");
		String region = response.getString("region");
		String regionName = region.equals("CT") ? "Local" : "CSEZ";
		String buildUrlMessage = "\n\nBuild URL : " + url;
		if(overallStatus.equals("Failed"))
		{
			message = "Build update failed in " + regionName + buildUrlMessage;
		}
		else
		{
			message = "Build update completed successfully in " + regionName + buildUrlMessage;
		}

		ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), messageID, serverRepoName, gitlabIssueID, "MASTER BUILD", message);
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
