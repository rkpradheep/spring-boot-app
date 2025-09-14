package com.server.framework.job;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.server.framework.entity.JobEntity;
import com.server.framework.service.JobService;
import com.server.framework.service.WorkflowService;
import com.server.framework.workflow.WorkflowEngine;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.model.WorkflowState;

@Component
@Scope("prototype")
public class WorkFlowTask implements Task
{
	private static final Logger LOGGER = Logger.getLogger(WorkFlowTask.class.getName());

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private JobService jobService;

	@Autowired
	WorkflowEngine workflowEngine;

	@Override public void run(long jobID) throws Exception
	{
		LOGGER.info("Executing WorkFlowTask for job ID: " + jobID);

		Optional<JobEntity> jobEntityOptional = jobService.getJob(jobID);
		if(jobEntityOptional.isEmpty())
		{
			LOGGER.log(Level.SEVERE, "Job not found for ID: " + jobID);
			return;
		}

		JobEntity jobEntity = jobEntityOptional.get();
		String data = jobEntity.getData();
		JSONObject jsonObject = new JSONObject(data);
		String instanceId = jsonObject.getString("instanceId");

		Optional<WorkflowInstance> workflowInstance = workflowService.getInstance(instanceId);
		if(workflowInstance.isEmpty())
		{
			LOGGER.log(Level.SEVERE, "Workflow instance not found for ID: " + instanceId);
			return;
		}

		if(workflowInstance.get().getStatus() == WorkflowState.COMPLETED || workflowInstance.get().getStatus() == WorkflowState.FAILED)
		{
			LOGGER.info("Workflow instance " + instanceId + " is already in terminal state: " + workflowInstance.get().getStatus());
			return;
		}

		workflowEngine.executeCurrentStep(workflowInstance.get());
	}
}
