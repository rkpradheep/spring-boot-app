package com.server.framework.workflow.executor;

import com.server.framework.common.DateUtil;
import com.server.framework.workflow.definition.WorkFlowCommonEventType;
import com.server.framework.workflow.definition.WorkflowStep;
import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Component
public class WorkflowExecutor
{

	private static final Logger LOGGER = Logger.getLogger(WorkflowExecutor.class.getName());

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	public WorkflowEvent executeStep(WorkflowStep step, WorkflowInstance instance)
	{
		LOGGER.info("Executing step: " + step.getName() + " for instance: " + instance.getReferenceID());

		try
		{
			if(step.isAsync())
			{
				return executeStepAsync(step, instance);
			}
			else
			{
				return executeStepSync(step, instance);
			}
		}
		catch(Exception e)
		{
			LOGGER.severe("Error executing step " + step.getName() + " for instance " + instance.getWorkflowName() + ": " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in executeStep", e);

			return new WorkflowEvent(WorkFlowCommonEventType.WORKFLOW_FAILED, Map.of("error", e.getMessage(), "step", step.getName()));
		}
	}

	private WorkflowEvent executeStepSync(WorkflowStep step, WorkflowInstance instance)
	{
		try
		{
			LOGGER.info("Executing step: " + step.getName() + " for instance: " + instance.getReferenceID() + " in same thread");

			WorkflowEvent result = step.execute(instance);

			LOGGER.info("Step " + step.getName() + " completed successfully for instance: " + instance.getReferenceID());
			return result;

		}
		catch(Exception e)
		{
			LOGGER.warning("Step " + step.getName() + " failed for instance: " + instance.getReferenceID() + ": " + e.getMessage());
			LOGGER.log(java.util.logging.Level.WARNING, "Exception in executeStepSync", e);
			return handleStepFailure(step, instance, e);
		}
	}

	private WorkflowEvent executeStepAsync(WorkflowStep step, WorkflowInstance instance)
	{
		java.util.concurrent.CompletableFuture.runAsync(() -> {
			try
			{
				WorkflowEvent result = step.execute(instance);
				if(result != null)
				{
					LOGGER.info("Async step " + step.getName() + " completed for instance: " + instance.getReferenceID());
				}
			}
			catch(Exception e)
			{
				LOGGER.severe("Async step " + step.getName() + " failed for instance: " + instance.getReferenceID() + ": " + e.getMessage());
				LOGGER.log(java.util.logging.Level.SEVERE, "Exception in executeStepAsync", e);
			}
		}, executorService);

		return null;
	}

	private WorkflowEvent handleStepFailure(WorkflowStep step, WorkflowInstance instance, Throwable error)
	{
		int retryCount = getRetryCount(instance, step.getName());

		if(retryCount < step.getMaxRetries())
		{
			LOGGER.info("Retrying step " + step.getName() + " (attempt " + (retryCount + 1) + "/" + step.getMaxRetries() + ") for instance: " + instance.getReferenceID());

			incrementRetryCount(instance, step.getName());

			try
			{
				Thread.sleep(Math.min(DateUtil.ONE_SECOND_IN_MILLISECOND * (long) Math.pow(2, retryCount), DateUtil.ONE_SECOND_IN_MILLISECOND * 30));
			}
			catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}

			return executeStep(step, instance);
		}
		else
		{
			LOGGER.warning("Step " + step.getName() + " failed after " + step.getMaxRetries() + " retries for instance: " + instance.getReferenceID());

			return new WorkflowEvent(step.getFailureEventName(), java.util.Map.of("error", "Step failed after max retries: " + error.getMessage(), "step", step.getName(), "retries", retryCount));
		}
	}

	private int getRetryCount(WorkflowInstance instance, String stepName)
	{
		String retryKey = "retry_count_" + stepName;
		String retryCount = instance.getVariable(retryKey);
		return StringUtils.isEmpty(retryCount) ? 0 : Integer.parseInt(retryCount);
	}

	private void incrementRetryCount(WorkflowInstance instance, String stepName)
	{
		String retryKey = "retry_count_" + stepName;
		int currentCount = getRetryCount(instance, stepName);
		instance.setVariable(retryKey, currentCount + 1);
	}

	public void shutdown()
	{
		executorService.shutdown();
	}
}
