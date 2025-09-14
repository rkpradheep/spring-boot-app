package com.server.framework.job;

import com.server.framework.common.AppProperties;
import com.server.framework.entity.JobEntity;
import com.server.framework.service.EmailService;
import com.server.framework.service.JobService;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MailTask implements Task
{

	@Autowired
	private EmailService emailService;

	@Autowired
	private JobService jobService;

	public void run(long jobID) throws Exception
	{
		JobEntity jobEntity = jobService.getJob(jobID).get();

		JSONObject jsonObject = new JSONObject(jobEntity.getData());
		String message = jsonObject.getString("message");
		message += "<br><br><b>Note : This email is sent on behalf of " + jsonObject.getString("from_address") + "</b>";

		if(jobEntity.getIsRecurring())
		{
			String appUrl = AppProperties.getProperty("app.url");
			if(StringUtils.isNotEmpty(appUrl))
			{
				message += "<br><br> Click <a href=\"" + appUrl + "/scheduler/delete?scheduler_token=" +
					DigestUtils.sha1Hex(String.valueOf(jobEntity.getId())) + "\">here</a> to unsubscribe";
			}
		}

		emailService.sendHtmlEmail(
			jsonObject.getString("subject"),
			jsonObject.getString("to_address"),
			jsonObject.getString("from_address"),
			message
		);
	}
}
