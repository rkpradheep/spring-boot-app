package com.server.zoho;

public class ProductConfig
{
	private final String id;
	private final String channel;
	private final String branch;
	private boolean isServerRepo;
	private String buildUrl;
	private int order;
	private String gitlabUrl;
	private String gitlabToken;
	private String parentProduct;
	private String gitlabIssueUrl;
	private String productName;
	private String patchBuildZipName;

	public ProductConfig(String productName, String id, String channel, String branch, boolean isServerRepo, String buildUrl, int order, String gitlabUrl, String gitlabToken, String parentProduct, String gitlabIssueUrl, String patchBuildZipName)
	{
		this.productName = productName;
		this.id = id;
		this.channel = channel;
		this.branch = branch;
		this.isServerRepo = isServerRepo;
		this.buildUrl = buildUrl;
		this.order = order;
		this.gitlabUrl = gitlabUrl;
		this.gitlabToken = gitlabToken;
		this.parentProduct = parentProduct;
		this.gitlabIssueUrl = gitlabIssueUrl;
		this.patchBuildZipName = patchBuildZipName;
	}

	public String getProductName()
	{
		return productName;
	}

	public String getId()
	{
		return id;
	}

	public String getChannel()
	{
		return channel;
	}

	public String getBranch()
	{
		return branch;
	}

	public boolean isServerRepo()
	{
		return isServerRepo;
	}

	public String getBuildUrl()
	{
		return buildUrl;
	}

	public int getOrder()
	{
		return order;
	}

	public String getGitlabUrl()
	{
		return gitlabUrl;
	}

	public String getGitlabToken()
	{
		return gitlabToken;
	}

	public String getParentProduct()
	{
		return parentProduct;
	}

	public String getGitlabIssueUrl()
	{
		return gitlabIssueUrl;
	}

	public String getPatchBuildZipName()
	{
		return patchBuildZipName;
	}
}
