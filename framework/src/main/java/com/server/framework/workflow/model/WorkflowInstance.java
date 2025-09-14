package com.server.framework.workflow.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WorkflowInstance
{

	private String referenceID;
	private String workflowName;
	private String currentState;
	private WorkflowState status;
	private Object context;
	private Long startTime;
	private Long endTime;
	private Long lastUpdateTime;
	private String errorMessage;
	private List<WorkflowEvent> eventHistory;
	private Map<String, Object> variables;

	public WorkflowInstance()
	{
		this.eventHistory = new ArrayList<>();
		this.variables = new HashMap<>();
		this.startTime = System.currentTimeMillis();
		this.lastUpdateTime = System.currentTimeMillis();
	}

	public String getReferenceID()
	{
		return referenceID;
	}

	public void setReferenceID(String referenceID)
	{
		this.referenceID = referenceID;
	}

	public String getWorkflowName()
	{
		return workflowName;
	}

	public void setWorkflowName(String workflowName)
	{
		this.workflowName = workflowName;
	}

	public String getCurrentState()
	{
		return currentState;
	}

	public void setCurrentState(String currentState)
	{
		this.currentState = currentState;
	}

	public WorkflowState getStatus()
	{
		return status;
	}

	public void setStatus(WorkflowState status)
	{
		this.status = status;
	}

	public Object getContext()
	{
		return context;
	}

	public void setContext(Object context)
	{
		this.context = context;
	}

	public Long getStartTime()
	{
		return startTime;
	}

	public void setStartTime(Long startTime)
	{
		this.startTime = startTime;
	}

	public Long getEndTime()
	{
		return endTime;
	}

	public void setEndTime(Long endTime)
	{
		this.endTime = endTime;
	}

	public Long getLastUpdateTime()
	{
		return lastUpdateTime;
	}

	public void setLastUpdateTime(Long lastUpdateTime)
	{
		this.lastUpdateTime = lastUpdateTime;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public List<WorkflowEvent> getEventHistory()
	{
		return eventHistory;
	}

	public void setEventHistory(List<WorkflowEvent> eventHistory)
	{
		this.eventHistory = eventHistory;
	}

	public Map<String, Object> getVariables()
	{
		return variables;
	}

	public void setVariables(Map<String, Object> variables)
	{
		this.variables = variables;
	}

	public void addEventToHistory(WorkflowEvent event)
	{
		this.eventHistory.add(event);
		this.lastUpdateTime = System.currentTimeMillis();
	}

	public void setVariable(String key, Object value)
	{
		this.variables.put(key, String.valueOf(value));
	}

	public String getVariable(String key)
	{
		return Objects.nonNull(this.variables.get(key)) ? this.variables.get(key).toString() : null;
	}

	public boolean hasVariable(String key)
	{
		return this.variables.containsKey(key);
	}

	public long getDuration()
	{
		if(endTime != null && startTime != null)
		{
			return endTime - startTime;
		}
		return System.currentTimeMillis() - startTime;
	}

	public boolean isRunning()
	{
		return status == WorkflowState.RUNNING;
	}

	public boolean isCompleted()
	{
		return status == WorkflowState.COMPLETED;
	}

	public boolean isFailed()
	{
		return status == WorkflowState.FAILED;
	}

	public boolean canRetry()
	{
		return status == WorkflowState.FAILED;
	}
}
