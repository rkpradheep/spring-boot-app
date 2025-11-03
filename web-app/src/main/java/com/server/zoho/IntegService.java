package com.server.zoho;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.server.framework.common.AppProperties;
import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.error.AppException;
import com.server.zoho.entity.BuildMonitorEntity;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;
import com.server.zoho.entity.BuildProductEntity;
import com.server.framework.workflow.WorkflowEngine;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class IntegService
{

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Autowired
	BuildMonitorService buildMonitorService;

	@Autowired
	BuildProductService buildProductService;

	@Lazy
	@Autowired
	WorkflowEngine workflowEngine;

	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(IntegService.class.getName());

	public IntegService()
	{
		this.restTemplate = new RestTemplate();
		this.objectMapper = new ObjectMapper();
		this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	public static Map<String, ProductConfig> getProductConfig()
	{
		return (Map<String, ProductConfig>) ZohoService.getMetaConfig("PRODUCT_CONFIGS");
	}

	public static ProductConfig getProductConfigForID(String productID)
	{
		Map<String, ProductConfig> productConfigMap = (Map<String, ProductConfig>) ZohoService.getMetaConfig("PRODUCT_CONFIGS");
		for(ProductConfig config : productConfigMap.values())
		{
			if(config.getId().equals(productID))
			{
				return config;
			}
		}

		return null;
	}

	public static ProductConfig getProductConfig(String productName)
	{
		return ((Map<String, ProductConfig>) ZohoService.getMetaConfig("PRODUCT_CONFIGS")).get(productName);
	}

	public static String getTodayBuildOwnerEmail()
	{
		return ((Map<String, String>) ZohoService.getMetaConfig("BUILD_OWNERS")).get(DateUtil.getFormattedCurrentTime("EEEE").toUpperCase());
	}

	public BuildResponse scheduleBuilds(Set<String> productNames)
	{

		if(productNames == null || productNames.isEmpty())
		{
			return new BuildResponse("Invalid request. No products specified");
		}

		List<String> invalidProducts = new ArrayList<>();
		for(String product : productNames)
		{
			if(Objects.isNull(getProductConfig(product)))
			{
				invalidProducts.add(product);
			}
		}

		if(!invalidProducts.isEmpty())
		{
			String supportedRepos = String.join(", ", getProductConfig().keySet());
			return new BuildResponse("Invalid repo names provided: " + String.join(", ", invalidProducts) + " Supported repo names : " + supportedRepos);
		}

		scheduleBuildWorkFlow(productNames);

		return new BuildResponse("Build workflow scheduled successfully for " + productNames.size() + " products");

	}

	public BuildResponse initiateBuild(String productName, boolean isPatchBuild, String branchName)
	{
		try
		{
			//Mock for testing
//						BuildResponse buildResponse = new BuildResponse(true, "Build has been started successfully", 11833705L, "RUNNING");
//						buildResponse.setProductId("4671");
//						buildResponse.setBuildType("FULLBUILD");
//						buildResponse.setCheckoutLabel("master");
//						buildResponse.setUrl("https://build.zohocorp.com/zoho/payout_mock_configuration/webhost/master/Oct_30_2025");
//						buildResponse.setBuildLogId(11833705L);
//						if(true)
//							return buildResponse;


			//Mock for testing server
			//						BuildResponse buildResponse = new BuildResponse(true, "Build has been started successfully", 11723376L, "RUNNING");
			//						buildResponse.setProductId("4670");
			//						buildResponse.setBuildType("FULLBUILD");
			//						buildResponse.setCheckoutLabel("master");
			//						buildResponse.setUrl("https://build.zohocorp.com/zoho/payout_server/webhost/master/Oct_16_2025_1/");
			//
			//						if(true)
			//							return buildResponse;

			Map<String, ProductConfig> productConfigMap = getProductConfig();
			if(!productConfigMap.containsKey(productName))
			{
				String supportedRepos = String.join(", ", productConfigMap.keySet());
				return new BuildResponse("Invalid repo name provided: " + productName +
					"\n\nSupported repo names : " + supportedRepos);
			}

			if(isPatchBuild)
			{
				return callPatchBuildApi(productName, branchName);
			}

			ProductConfig config = productConfigMap.get(productName);
			String productId = config.getId();
			String userNames = "pradheep.rkd";

			BuildRequest buildRequest = createBuildRequest(config.getBranch(), productId, userNames);
			return callBuildApi(buildRequest);

		}
		catch(Exception e)
		{
			return new BuildResponse("Request failed. Please try again later. Error: " + e.getMessage());
		}
	}

	private void scheduleBuildWorkFlow(Set<String> productNames)
	{
		try
		{
			BuildMonitorEntity monitor = buildMonitorService.createBuildMonitor(productNames);

			BuildProductEntity firstProduct = buildProductService.getNextPendingProduct(monitor.getId()).orElse(null);

			String message = DateUtil.getFormattedCurrentTime("'Master Build' dd MMMM yyyy").toUpperCase() + " (" + monitor.getId() +  ")";
			String messageID;
			String gitlabIssueID;
			List<String> qualifiedProducts = buildProductService.getProductsForMonitor(monitor.getId()).stream().map(BuildProductEntity::getProductName).collect(Collectors.toList());

			Map<String, Object> context = new HashMap<>()
			{
				{
					put("monitorId", monitor.getId());
					put("productId", firstProduct.getId());
				}
			};

			Optional<String> serverRepoOptional = qualifiedProducts.stream().filter(productName -> productName.endsWith("_server")).findFirst();
			if(serverRepoOptional.isPresent())
			{
				messageID = ZohoService.postMessageToChannel(CommonService.getDefaultChannelUrl(), message + "\n{@participants}");
				context.put("messageID", messageID);
				gitlabIssueID = ZohoService.createIssue(serverRepoOptional.get(), message);
				context.put("gitlabIssueID", gitlabIssueID);
				context.put("server_product_name", serverRepoOptional.get());
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, message, "[GITLAB ISSUE CREATED](https://zpaygit.csez.zohocorpin.com/zohopay/" + serverRepoOptional.get() + "/-/issues/" + gitlabIssueID + ")");

				ZohoService.postChangeSet(ZohoService.generatePayoutChangSetsFromIDC(), CommonService.getDefaultChannelUrl(), context);
			}

			String buildOwnerEmail = IntegService.getTodayBuildOwnerEmail();
			if(StringUtils.isNotEmpty(buildOwnerEmail))
			{
				ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, "MASTER BUILD", "Build Owner : {@" + buildOwnerEmail + "}");
			}

			ZohoService.createOrSendMessageToThread(CommonService.getDefaultChannelUrl(), context, message, "Products qualified for build : " + String.join(",", qualifiedProducts));

			String referenceID = monitor.getId().toString();
			workflowEngine.scheduleWorkflow("BuildWorkflow", referenceID, context, ZohoService.getCurrentUserEmail());

			LOGGER.info("Scheduled workflow instance:  for monitor: " + monitor.getId() + " and product: " + firstProduct.getProductName());

		}
		catch(Exception e)
		{
			LOGGER.severe("Error scheduling build monitoring: " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in scheduleBuildMonitoring", e);
			throw new AppException("Failed to schedule build monitoring");
		}
	}

	private BuildRequest createBuildRequest(String branchName, String productId, String userNames)
	{
		BuildLog buildLog = new BuildLog();
		buildLog.setCheckoutLabel(branchName);
		buildLog.setStatus("Start");
		buildLog.setReportNeeded("true");
		buildLog.setBuildType("FULLBUILD");
		buildLog.setStartedFrom("Webhost");
		buildLog.setCustomizeInfo("JAVA_VERSION=JAVA17,BUILD_TASKS=COMPLETE,PATCH_BUILD_CHANGESET=NA");
		buildLog.setSuccessMail(userNames);
		buildLog.setErrorMail(userNames);
		buildLog.setSecurityReportNeeded("true");
		buildLog.setSelected("true");
		buildLog.setProductId(productId);
		buildLog.setInstantResponse("true");

		BuildRequest request = new BuildRequest();
		request.setBuildLog(buildLog);
		return request;
	}

	public BuildResponse checkBuildStatus(Long buildId)
	{
		try
		{
			String apiToken = AppProperties.getProperty("zoho.build.api.token");
			String statusUrl = AppProperties.getProperty("zoho.build.api.url") + "/api/v1/buildlogs?id=" + buildId;

			HttpHeaders headers = new HttpHeaders();
			headers.set("PRIVATE-TOKEN", apiToken);
			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<String> response = restTemplate.exchange(
				statusUrl,
				HttpMethod.GET,
				entity,
				String.class
			);

			JsonNode responseNode = objectMapper.readTree(response.getBody());

			if(responseNode.has("buildlogs") && responseNode.get("buildlogs").isArray())
			{
				JsonNode buildlogsArray = responseNode.get("buildlogs");
				if(!buildlogsArray.isEmpty())
				{
					JsonNode buildlog = buildlogsArray.get(0);
					if(buildlog.has("status"))
					{
						String status = buildlog.get("status").asText();
						Long logId = buildlog.has("id") ? buildlog.get("id").asLong() : buildId;
						String productId = buildlog.has("product_id") ? buildlog.get("product_id").asText() : null;
						String buildType = buildlog.has("build_type") ? buildlog.get("build_type").asText() : null;
						String checkoutLabel = buildlog.has("checkout_label") ? buildlog.get("checkout_label").asText() : null;
						String duration = buildlog.has("duration") ? buildlog.get("duration").asText() : null;
						String url = buildlog.has("url") ? buildlog.get("url").asText() : null;
						String releaseVersion = buildlog.has("release_version") && !buildlog.get("release_version").isNull() ? buildlog.get("release_version").asText() : null;

						BuildResponse buildResponse = new BuildResponse(true, "Build status retrieved", logId, status);
						buildResponse.setProductId(productId);
						buildResponse.setBuildType(buildType);
						buildResponse.setCheckoutLabel(checkoutLabel);
						buildResponse.setDuration(duration);
						buildResponse.setUrl(url);
						buildResponse.setReleaseVersion(releaseVersion);

						if("Success".equals(status))
						{
							buildResponse.setMessage("BUILD_SUCCESS");
						}
						else if("Failure".equals(status) || "Error".equals(status))
						{
							buildResponse.setMessage("BUILD_FAILED");
							buildResponse.setSuccess(false);
						}
						else
						{
							buildResponse.setMessage("BUILD_IN_PROGRESS");
						}

						return buildResponse;
					}
				}
			}

			return new BuildResponse(false, "UNKNOWN");
		}
		catch(Exception e)
		{
			LOGGER.warning("Failed to check build status for ID " + buildId + ": " + e.getMessage());
			return new BuildResponse(false, "ERROR", null, null);
		}
	}

	private BuildResponse callBuildApi(BuildRequest request)
	{
		try
		{
			String apiToken = AppProperties.getProperty("zoho.build.api.token");
			String buildApiUrl = AppProperties.getProperty("zoho.build.api.url") + "/api/v1/buildlogs";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("PRIVATE-TOKEN", apiToken);

			String jsonPayload = objectMapper.writeValueAsString(request);
			HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

			ResponseEntity<String> response = restTemplate.exchange(
				buildApiUrl,
				HttpMethod.POST,
				entity,
				String.class
			);

			BuildResponse buildResponse = getBuildResponse(response.getBody());

			if(Objects.nonNull(buildResponse))
			{
				return buildResponse;
			}
			return new BuildResponse(false, "Request failed. Please try again later.");

		}
		catch(Exception e)
		{
			return new BuildResponse(false, "Request failed. Please try again later.");
		}
	}

	private BuildResponse callPatchBuildApi(String product, String branchName)
	{
		try
		{
			String apiToken = AppProperties.getProperty("zoho.build.api.token");
			String buildApiUrl = AppProperties.getProperty("zoho.build.api.url") + "/api/v1/patch_build_details";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("PRIVATE-TOKEN", apiToken);

			JSONObject request = new JSONObject()
				.put("stage", "pre")
				.put("grid_value", "IN2")
				.put("product_id", IntegService.getProductConfig(product).getId())
				.put("checkout_label", branchName)
				.put("product_name", "ZOHOPAYOUT")
				.put("patch_build_url", ZohoService.getPatchBuildURL(product, "pre"));

			HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);

			ResponseEntity<String> response = restTemplate.exchange(
				buildApiUrl,
				HttpMethod.POST,
				entity,
				String.class
			);

			BuildResponse buildResponse = getBuildResponse(response.getBody());

			if(Objects.nonNull(buildResponse))
			{
				return buildResponse;
			}
			return new BuildResponse(false, "Request failed. Please try again later.");

		}
		catch(Exception e)
		{
			return new BuildResponse(false, "Request failed. Please try again later.");
		}
	}

	public BuildResponse getBuildResponse(String response) throws JsonProcessingException
	{
		JsonNode responseNode = objectMapper.readTree(response);

		if(!responseNode.has("buildlogs") && !responseNode.has("patch_build_details"))
		{
			return null;
		}
		JsonNode buildlogs = responseNode.has("patch_build_details") ? responseNode.get("patch_build_details") : responseNode.get("buildlogs");
		buildlogs = buildlogs.isArray() ? buildlogs.get(0) : buildlogs;

		Long buildId = buildlogs.get("id").asLong();
		String status = buildlogs.has("status") ? buildlogs.get("status").asText() : "Unknown";
		String productId = buildlogs.has("product_id") ? buildlogs.get("product_id").asText() : null;
		String buildType = buildlogs.has("build_type") ? buildlogs.get("build_type").asText() : null;
		String checkoutLabel = buildlogs.has("checkout_label") ? buildlogs.get("checkout_label").asText() : null;
		String duration = buildlogs.has("duration") ? buildlogs.get("duration").asText() : null;
		String url = buildlogs.has("url") ? buildlogs.get("url").asText() : null;
		String commit = buildlogs.has("build_label") ? buildlogs.get("build_label").asText() : null;
		String releaseVersion = buildlogs.has("release_version") ? buildlogs.get("release_version").asText() : null;
		String patchBuildURL = buildlogs.has("patch_build_url") ? buildlogs.get("patch_build_url").asText() : null;

		BuildResponse buildResponse = new BuildResponse(true, "Build has been started successfully", buildId, status);
		buildResponse.setProductId(productId);
		buildResponse.setBuildType(buildType);
		buildResponse.setCheckoutLabel(checkoutLabel);
		buildResponse.setDuration(duration);
		buildResponse.setUrl(url);
		buildResponse.setBuildLogId(buildId);
		buildResponse.setCommitSHA(commit);
		buildResponse.setReleaseVersion(releaseVersion);
		buildResponse.setPatchBuildUrl(patchBuildURL);

		return buildResponse;
	}

}
