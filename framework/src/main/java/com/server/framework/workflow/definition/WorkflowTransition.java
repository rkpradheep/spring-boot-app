package com.server.framework.workflow.definition;

public class WorkflowTransition
{

	private String fromState;
	private String toState;
	private String eventType;
	private String condition;
	private boolean terminal;
	private String description;

	public WorkflowTransition()
	{
		this.terminal = false;
	}

	public WorkflowTransition(String fromState, String toState, String eventType)
	{
		this();
		this.fromState = fromState;
		this.toState = toState;
		this.eventType = eventType;
	}

	public WorkflowTransition(String fromState, String toState, String eventType, boolean terminal)
	{
		this(fromState, toState, eventType);
		this.terminal = terminal;
	}

	public boolean isConditionMet(Object context)
	{
		if(condition == null || condition.trim().isEmpty())
		{
			return true;
		}

		return true;
	}

	public String getFromState()
	{
		return fromState;
	}

	public void setFromState(String fromState)
	{
		this.fromState = fromState;
	}

	public String getToState()
	{
		return toState;
	}

	public void setToState(String toState)
	{
		this.toState = toState;
	}

	public String getEventType()
	{
		return eventType;
	}

	public void setEventType(String eventType)
	{
		this.eventType = eventType;
	}

	public String getCondition()
	{
		return condition;
	}

	public void setCondition(String condition)
	{
		this.condition = condition;
	}

	public boolean isTerminal()
	{
		return terminal;
	}

	public void setTerminal(boolean terminal)
	{
		this.terminal = terminal;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	public String toString()
	{
		return String.format("Transition[%s -> %s on %s]", fromState, toState, eventType);
	}
}
