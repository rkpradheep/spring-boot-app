package com.server.zoho.workflow.model;

public enum BuildStates
{
	BUILD_INITIATION(Constants.BUILD_INITIATION),
	BUILD_IN_PROGRESS(Constants.BUILD_IN_PROGRESS),
	BUILD_COMPLETED(Constants.BUILD_COMPLETED),
	BUILD_FAILED(Constants.BUILD_FAILED),
	MILESTONE_CREATION(Constants.MILESTONE_CREATION),
	MILESTONE_FAILED(Constants.MILESTONE_FAILED),
	CHANNEL_MAPPING(Constants.CHANNEL_MAPPING),
	CHANNEL_FAILED(Constants.CHANNEL_FAILED),
	NEXT_PRODUCT(Constants.NEXT_PRODUCT),
	WORKFLOW_COMPLETED(Constants.WORKFLOW_COMPLETED),
	WORKFLOW_FAILED(Constants.WORKFLOW_FAILED),
	SD_CSEZ_BUILD_UPLOAD(Constants.SD_CSEZ_BUILD_UPLOAD),
	SD_CSEZ_BUILD_UPLOAD_FAILED(Constants.SD_CSEZ_BUILD_UPLOAD_FAILED),
	SD_LOCAL_BUILD_UPLOAD(Constants.SD_LOCAL_BUILD_UPLOAD),
	SD_LOCAL_BUILD_UPLOAD_FAILED(Constants.SD_LOCAL_BUILD_UPLOAD_FAILED);

	public final String value;

	BuildStates(String value)
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

	public static BuildStates fromString(String value)
	{
		for(BuildStates state : BuildStates.values())
		{
			if(state.value.equals(value))
			{
				return state;
			}
		}
		throw new IllegalArgumentException("Unknown state: " + value);
	}

	public static class Constants
	{
		public static final String BUILD_INITIATION = "BUILD_INITIATION";
		public static final String BUILD_IN_PROGRESS = "BUILD_IN_PROGRESS";
		public static final String BUILD_COMPLETED = "BUILD_COMPLETED";
		public static final String BUILD_FAILED = "BUILD_FAILED";
		public static final String MILESTONE_CREATION = "MILESTONE_CREATION";
		public static final String MILESTONE_FAILED = "MILESTONE_FAILED";
		public static final String CHANNEL_MAPPING = "CHANNEL_MAPPING";
		public static final String CHANNEL_FAILED = "CHANNEL_FAILED";
		public static final String NEXT_PRODUCT = "NEXT_PRODUCT";
		public static final String WORKFLOW_COMPLETED = "WORKFLOW_COMPLETED";
		public static final String WORKFLOW_FAILED = "WORKFLOW_FAILED";
		public static final String SD_CSEZ_BUILD_UPLOAD = "SD_CSEZ_BUILD_UPLOAD";
		public static final String SD_CSEZ_BUILD_UPLOAD_FAILED = "SD_CSEZ_BUILD_UPLOAD_FAILED";
		public static final String SD_LOCAL_BUILD_UPLOAD = "SD_LOCAL_BUILD_UPLOAD";
		public static final String SD_LOCAL_BUILD_UPLOAD_FAILED = "SD_LOCAL_BUILD_UPLOAD_FAILED";
	}
}
