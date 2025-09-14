package com.server.zoho.workflow.model;

import com.server.framework.workflow.definition.WorkFlowEventType;

public enum BuildEventType implements WorkFlowEventType
{
	BUILD_STARTED("BUILD_STARTED"),
	BUILD_COMPLETED("BUILD_COMPLETED"),
	BUILD_FAILED("BUILD_FAILED"),
	MILESTONE_CREATION("MILESTONE_CREATION"),
	CHANNEL_MAPPING("CHANNEL_MAPPING"),
	MILESTONE_FAILED("MILESTONE_FAILED"),
	NEXT_PRODUCT("NEXT_PRODUCT"),
	CHANNEL_FAILED("CHANNEL_FAILED"),
	RETRY_REQUESTED("RETRY_REQUESTED"),
	BUILD_STATUS_CHECK("BUILD_STATUS_CHECK"),
	SD_BUILD_UPLOAD("SD_BUILD_UPLOAD");

	private final String value;

	BuildEventType(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	@Override
	public String toString()
	{
		return value;
	}

	public static BuildEventType fromString(String value)
	{
		for(BuildEventType eventType : BuildEventType.values())
		{
			if(eventType.value.equals(value))
			{
				return eventType;
			}
		}
		throw new IllegalArgumentException("Unknown event type: " + value);
	}
}
