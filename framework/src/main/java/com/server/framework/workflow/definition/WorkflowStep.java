package com.server.framework.workflow.definition;

import com.server.framework.workflow.model.WorkflowEvent;
import com.server.framework.workflow.model.WorkflowInstance;

public abstract class WorkflowStep
{
	private String name;
	private String description;
	private StepType type;
	private int timeoutSeconds;
	private int maxRetries;
	private boolean async;
	private String actionClass;

	public enum StepType
	{
		ACTION,     // Execute an action
		DECISION,   // Make a decision based on condition
		PARALLEL,   // Execute multiple actions in parallel
		TIMER,      // Wait for a specified time
	}

	public WorkflowStep()
	{
		this.timeoutSeconds = 300;
		this.maxRetries = 3;
		this.async = false;
		this.type = StepType.ACTION;
	}

	public abstract WorkflowEvent execute(WorkflowInstance instance);

	public String getFailureEventName()
	{
		return WorkFlowCommonEventType.WORKFLOW_FAILED.getValue();
	}

	public WorkflowEvent handleTimeout(WorkflowInstance instance)
	{
		return new WorkflowEvent(WorkFlowCommonEventType.TIMEOUT);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public StepType getType()
	{
		return type;
	}

	public void setType(StepType type)
	{
		this.type = type;
	}

	public int getTimeoutSeconds()
	{
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds)
	{
		this.timeoutSeconds = timeoutSeconds;
	}

	public int getMaxRetries()
	{
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries)
	{
		this.maxRetries = maxRetries;
	}

	public boolean isAsync()
	{
		return async;
	}

	public void setAsync(boolean async)
	{
		this.async = async;
	}

	public String getActionClass()
	{
		return actionClass;
	}

	public void setActionClass(String actionClass)
	{
		this.actionClass = actionClass;
	}
}
