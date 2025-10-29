package com.server.zoho;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.framework.job.Task;

@Service
public class PayoutBuildAutomationTask implements Task
{
	@Autowired
	BuildAutomationService buildAutomationService;
	@Override public void run(long jobID) throws Exception
	{
		buildAutomationService.startBuildAutomationForPayout();
	}
}
