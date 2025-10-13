package com.server.zoho.workflow.model;

public enum BuildProductStatus
{

	PENDING("PENDING"),
	BUILD_STARTED("BUILD_STARTED"),
	BUILD_SUCCESS("BUILD_SUCCESS"),
	BUILD_FAILED("BUILD_FAILED"),
	CHANNEL_MAPPED("CHANNEL_MAPPED"),
	CHANNEL_FAILED("CHANNEL_FAILED"),
	MILESTONE_CREATED("MILESTONE_CREATED"),
	MILESTONE_FAILED("MILESTONE_FAILED"),
	SD_BUILD_UPLOADED("SD_BUILD_UPLOADED"),
	CANCELLED("CANCELLED");

	private String name;

	BuildProductStatus(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
