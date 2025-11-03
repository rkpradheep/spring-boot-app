package com.server.zoho;

public class BuildResponse
{
	private boolean success;
	private String message;
	private Long buildLogId;
	private String status;
	private String buildType;
	private String checkoutLabel;
	private String productId;
	private String duration;
	private String url;
	private String errorMessage;
	private String releaseVersion;
	private String commitSHA;
	private String patchBuildUrl;

	public BuildResponse(boolean success, String message)
	{
		this.success = success;
		this.message = message;
	}

	public BuildResponse(boolean success, String message, Long buildLogId, String status)
	{
		this.success = success;
		this.message = message;
		this.buildLogId = buildLogId;
		this.status = status;
	}

	public BuildResponse(String text)
	{
		this.success = text.contains("successfully");
		this.message = text;
	}

	public boolean isSuccess()
	{
		return success;
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Long getBuildLogId()
	{
		return buildLogId;
	}

	public void setBuildLogId(Long buildLogId)
	{
		this.buildLogId = buildLogId;
	}

	public void setCommitSHA(String commitSHA)
	{
		this.commitSHA = commitSHA;
	}

	public String getCommitSHA()
	{
		return commitSHA;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getBuildType()
	{
		return buildType;
	}

	public void setBuildType(String buildType)
	{
		this.buildType = buildType;
	}

	public String getCheckoutLabel()
	{
		return checkoutLabel;
	}

	public void setCheckoutLabel(String checkoutLabel)
	{
		this.checkoutLabel = checkoutLabel;
	}

	public void setReleaseVersion(String releaseVersion)
	{
		this.releaseVersion = releaseVersion;
	}

	public void setPatchBuildUrl(String patchBuildUrl)
	{
		this.patchBuildUrl = patchBuildUrl;
	}

	public String getPatchBuildUrl()
	{
		return this.patchBuildUrl;
	}

	public String getReleaseVersion()
	{
		return releaseVersion;
	}

	public String getProductId()
	{
		return productId;
	}

	public void setProductId(String productId)
	{
		this.productId = productId;
	}

	public String getDuration()
	{
		return duration;
	}

	public void setDuration(String duration)
	{
		this.duration = duration;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public String getText()
	{
		return message != null ? message : "Unknown";
	}

	public void setText(String text)
	{
		this.message = text;
	}
}
