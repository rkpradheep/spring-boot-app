package com.server.framework.workflow.definition;

public enum WorkFlowCommonEventType implements WorkFlowEventType
{
	WORKFLOW_COMPLETED("WORKFLOW_COMPLETED"),
	TIMEOUT("TIMEOUT"),
	WORKFLOW_FAILED("WORKFLOW_FAILED"),
	WORKFLOW_CANCELLED("WORKFLOW_CANCELLED");

	private final String value;

	WorkFlowCommonEventType(String value)
	{
		this.value = value;
	}

	@Override public String getValue()
	{
		return value;
	}
}
