package com.server.framework.workflow;

import com.server.framework.common.DateUtil;
import com.server.framework.job.TaskEnum;
import com.server.framework.service.JobService;
import com.server.framework.service.WorkflowService;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.framework.workflow.definition.WorkflowDefinition;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.framework.workflow.definition.WorkflowTransition;
import com.server.framework.workflow.executor.WorkflowExecutor;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;
import com.server.framework.workflow.model.WorkflowStatus;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service public class WorkflowEngine
{

	private static final Logger LOGGER = Logger.getLogger(WorkflowEngine.class.getName());

	@Autowired private WorkflowService workflowService;

	@Autowired private WorkflowExecutor workflowExecutor;

	@Autowired private List<WorkflowDefinition> workflowDefinitions;

	@Autowired private JobService jobService;

	public void scheduleWorkflow(String workflowName, String ReferenceId, Object context, String createdBy) throws Exception
	{
		LOGGER.info("Starting workflow: " + workflowName + " with instance ID: " + ReferenceId);

		WorkflowDefinition definition = findWorkflowDefinition(workflowName);
		if(definition == null)
		{
			throw new IllegalArgumentException("Workflow definition not found: " + workflowName);
		}

		WorkflowInstance instance = new WorkflowInstance();
		instance.setReferenceID(ReferenceId);
		instance.setWorkflowName(workflowName);
		instance.setCurrentState(definition.getInitialState());
		instance.setContext(context);
		instance.setStatus(WorkflowStatus.RUNNING);
		instance.setStartTime(System.currentTimeMillis());
		instance.setLastUpdateTime(System.currentTimeMillis());
		instance.setCreatedBy(createdBy);
		instance.setLastModifiedBy(createdBy);

		workflowService.saveInstance(instance);

		String data = new JSONObject().put("instanceId", instance.getReferenceID()).toString();
		jobService.scheduleJob(TaskEnum.WORKFLOW_TASK.getTaskName(), data, DateUtil.ONE_SECOND_IN_MILLISECOND);
	}

	public void processEvent(String instanceId, WorkflowEvent event)
	{
		LOGGER.info("Processing event: " + event.getEventType() + " for instance: " + instanceId);

		Optional<WorkflowInstance> instanceOpt = workflowService.getInstance(instanceId);
		if(instanceOpt.isEmpty())
		{
			LOGGER.warning("Workflow instance not found: " + instanceId);
			return;
		}

		WorkflowInstance instance = instanceOpt.get();
		WorkflowDefinition definition = findWorkflowDefinition(instance.getWorkflowName());

		List<WorkflowTransition> validTransitions = definition.getValidTransitions(instance.getCurrentState(), event.getEventType());

		if(validTransitions.isEmpty())
		{
			instance.setStatus(WorkflowStatus.FAILED);
			workflowService.saveInstance(instance);
			LOGGER.warning("No valid transitions found for state: " + instance.getCurrentState() + " and event: " + event.getEventType());
			return;
		}

		WorkflowTransition transition = validTransitions.get(0);
		executeTransition(instance, transition, event);
	}

	public void retryWorkflow(String instanceId)
	{
		LOGGER.info("Retrying workflow instance: " + instanceId);

		Optional<WorkflowInstance> instanceOpt = workflowService.getInstance(instanceId);
		if(instanceOpt.isEmpty())
		{
			LOGGER.warning("Workflow instance not found for retry: " + instanceId);
			return;
		}

		WorkflowInstance instance = instanceOpt.get();

		if(instance.getStatus() != WorkflowStatus.FAILED)
		{
			LOGGER.warning("Cannot retry workflow instance that is not in FAILED state: " + instanceId);
			return;
		}

		instance.setStatus(WorkflowStatus.RUNNING);
		instance.setLastUpdateTime(System.currentTimeMillis());
		instance.setErrorMessage(null);

		workflowService.saveInstance(instance);

		executeCurrentStep(instance);
	}

	public void executeCurrentStep(WorkflowInstance instance)
	{
		try
		{
			if(instance.getStatus() == WorkflowStatus.SUSPENDED || instance.getStatus() == WorkflowStatus.CANCELLED || instance.getStatus() == WorkflowStatus.COMPLETED)
			{
				LOGGER.info("Skipping the process. Workflow instance is in terminal state: " + instance.getStatus());
				return;
			}

			WorkflowDefinition definition = findWorkflowDefinition(instance.getWorkflowName());
			WorkflowStep currentStep = definition.getStep(instance.getCurrentState());

			if(currentStep == null)
			{
				LOGGER.warning("No step definition found for state: " + instance.getCurrentState());
				instance.setStatus(WorkflowStatus.FAILED);
				instance.setErrorMessage("No step definition found for state: " + instance.getCurrentState());
				workflowService.saveInstance(instance);
				return;
			}

			LOGGER.info("Executing step: " + currentStep.getName() + " for instance: " + instance.getReferenceID());

			WorkflowEvent result = workflowExecutor.executeStep(currentStep, instance);
			workflowService.saveInstance(instance);

			if(result != null)
			{
				processEvent(instance.getReferenceID(), result);
			}

		}
		catch(Exception e)
		{
			LOGGER.severe("Error executing step for instance " + instance.getReferenceID() + ": " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in executeStep", e);

			instance.setStatus(WorkflowStatus.FAILED);
			instance.setErrorMessage("Step execution failed: " + e.getMessage());
			instance.setLastUpdateTime(System.currentTimeMillis());
			workflowService.saveInstance(instance);
		}
	}

	private void executeTransition(WorkflowInstance instance, WorkflowTransition transition, WorkflowEvent event)
	{
		LOGGER.info("Executing transition: " + transition.getFromState() + " -> " + transition.getToState() + " for instance: " + instance.getReferenceID());

		try
		{
			instance.setCurrentState(transition.getToState());
			instance.setLastUpdateTime(System.currentTimeMillis());

			workflowService.saveEvent(event, instance.getReferenceID());

			if(transition.isTerminal())
			{
				WorkflowStatus workflowStatus = event.getEventType().equals(WorkFlowCommonEventType.WORKFLOW_FAILED.getValue()) ? WorkflowStatus.FAILED : WorkflowStatus.COMPLETED;
				if(workflowStatus == WorkflowStatus.COMPLETED)
				{
					instance.setEventHistory(null);
				}
				instance.setStatus(workflowStatus);
				instance.setEndTime(System.currentTimeMillis());
			}

			workflowService.saveInstance(instance);

			if(!transition.isTerminal())
			{
				executeCurrentStep(instance);
			}

		}
		catch(Exception e)
		{
			LOGGER.severe("Error executing transition for instance " + instance.getReferenceID() + ": " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in executeTransition", e);

			instance.setStatus(WorkflowStatus.FAILED);
			instance.setErrorMessage("Transition execution failed: " + e.getMessage());
			instance.setLastUpdateTime(System.currentTimeMillis());
			workflowService.saveInstance(instance);
		}
	}

	private WorkflowDefinition findWorkflowDefinition(String workflowName)
	{
		return workflowDefinitions.stream().filter(def -> def.getName().equals(workflowName)).findFirst().orElse(null);
	}

	public Optional<WorkflowInstance> getInstance(String instanceId)
	{
		return workflowService.getInstance(instanceId);
	}

	public List<WorkflowInstance> getAllInstances()
	{
		return workflowService.getAllInstances();
	}

	public List<WorkflowInstance> getInstancesByStatus(WorkflowStatus status)
	{
		return workflowService.getInstancesByStatus(status);
	}

	public void deleteWorkflow(String instanceId)
	{
		LOGGER.info("Deleting workflow instance: " + instanceId);

		try
		{
			workflowService.deleteInstance(instanceId);

			LOGGER.info("Workflow instance deleted successfully: " + instanceId);

		}
		catch(Exception e)
		{
			LOGGER.severe("Error deleting workflow instance " + instanceId + ": " + e.getMessage());
			throw new RuntimeException("Failed to delete workflow instance", e);
		}
	}
}
