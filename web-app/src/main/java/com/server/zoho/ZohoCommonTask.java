package com.server.zoho;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.http.HttpContext;
import com.server.framework.http.HttpResponse;
import com.server.framework.http.HttpService;
import com.server.framework.job.Task;

@Service
public class ZohoCommonTask implements Task
{
	private boolean canAddJobAgain;

	@Autowired HttpService httpService;

	private static long lastExecutionTime = DateUtil.getCurrentTimeInMillis();

	private static final Logger LOGGER = Logger.getLogger(ZohoCommonTask.class.getName());

	@Override public void run(long jobID) throws Exception
	{
		HttpContext httpContext = new HttpContext("https://books.zoho.in/_app/health", "GET");

		String headerString = "";

		httpContext.setHeadersMap(httpService.convertRawHeadersToMap(headerString));

		HttpResponse httpResponse = httpService.makeNetworkCall(httpContext);
		LOGGER.log(Level.INFO, "Response : {0}", httpResponse.getStringResponse());
		if(httpResponse.getStatus() == 400)
		{
			canAddJobAgain = true;
			if(lastExecutionTime < (DateUtil.getCurrentTimeInMillis() - (DateUtil.ONE_MINUTE_IN_MILLISECOND) * 15))
			{
				CommonService.postMessageToChannel("Zoho Common Task failed with 400 response. Adding job again for next schedule. Response : " + httpResponse.getStringResponse());
				lastExecutionTime = DateUtil.getCurrentTimeInMillis();
			}
			LOGGER.info("Job added again for next schedule.");
		}
		else
		{
			canAddJobAgain = false;
			CommonService.postMessageToChannel("Zoho Common Task completed successfully. Response : " + httpResponse.getStringResponse());
			LOGGER.info("Job completed successfully!");
		}
	}

	@Override public boolean canAddJobAgain()
	{
		return canAddJobAgain;
	}

	@Override public long getDelayBeforeAddJobAgain()
	{
		return DateUtil.ONE_MINUTE_IN_MILLISECOND;
	}

}
