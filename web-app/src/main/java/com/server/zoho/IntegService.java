package com.server.zoho;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.server.framework.common.AppProperties;
import com.server.framework.error.AppException;
import com.server.zoho.entity.BuildMonitorEntity;
import com.server.zoho.service.BuildMonitorService;
import com.server.zoho.service.BuildProductService;
import com.server.zoho.entity.BuildProductEntity;
import com.server.framework.workflow.WorkflowEngine;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

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

	private static Map<String, ProductConfig> PRODUCT_CONFIG_MAP;

	public IntegService()
	{
		this.restTemplate = new RestTemplate();
		this.objectMapper = new ObjectMapper();
		this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		loadProductConfiguration();
	}

	private void loadProductConfiguration()
	{
		try
		{
			if(Objects.isNull(getClass().getClassLoader().getResource("zoho-properties.yml")))
			{
				LOGGER.info("zoho-properties.yml not found in classpath. Skipping product configuration load.");
				return;
			}
			PRODUCT_CONFIG_MAP = new HashMap<>();

			try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream("zoho-properties.yml"))
			{
				Yaml yaml = new Yaml();
				Map<String, Object> data = yaml.load(inputStream);

				if(!data.containsKey("integ_products"))
				{
					LOGGER.warning("No 'integ_products' section found in zoho-properties.yml. Skipping product configuration load.");
					return;
				}

				@SuppressWarnings("unchecked")
				List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("integ_products");

				for(Map<String, Object> product : products)
				{
					for(Map.Entry<String, Object> entry : product.entrySet())
					{
						String productName = entry.getKey();
						@SuppressWarnings("unchecked")
						Map<String, Object> config = (Map<String, Object>) entry.getValue();

						String productId = config.get("product_id").toString();
						String channelName = config.get("channel_name") != null ? config.get("channel_name").toString() : null;
						String branchName = config.get("branch_name").toString();
						boolean isServerRepo = config.get("is_server_repo") != null && Boolean.parseBoolean(config.get("is_server_repo").toString());
						String buildUrl = config.get("build_url") != null ? config.get("build_url").toString() : null;

						PRODUCT_CONFIG_MAP.put(productName, new ProductConfig(productId, channelName, branchName, isServerRepo, buildUrl));
					}
				}

				LOGGER.info("Loaded " + PRODUCT_CONFIG_MAP.size() + " product configurations from YAML");
			}
		}
		catch(Exception e)
		{
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in loadProductConfiguration", e);
		}
	}

	public static Map<String, ProductConfig> getProductConfigMap()
	{
		return PRODUCT_CONFIG_MAP != null ? PRODUCT_CONFIG_MAP : new HashMap<>();
	}

	public static ProductConfig getProductConfig(String productName)
	{
		return PRODUCT_CONFIG_MAP.get(productName);
	}

	public BuildResponse scheduleBuilds(List<String> productNames)
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
			String supportedRepos = String.join(", ", getProductConfigMap().keySet());
			return new BuildResponse("Invalid repo names provided: " + String.join(", ", invalidProducts) + " Supported repo names : " + supportedRepos);
		}

		scheduleBuildWorkFlow(productNames);

		return new BuildResponse("Build workflow scheduled successfully for " + productNames.size() + " products");

	}

	public BuildResponse initiateBuild(String productName)
	{
		try
		{
			//Mock for testing
//			BuildResponse buildResponse = new BuildResponse(true, "Build has been started successfully", 11646340L, "RUNNING");
//			buildResponse.setProductId("4671");
//			buildResponse.setBuildType("FULLBUILD");
//			buildResponse.setCheckoutLabel("master");
//			buildResponse.setUrl("https://build.zohocorp.com/zoho/payout_mock_configuration/webhost/master/Oct_07_2025_2");
//			buildResponse.setBuildLogId(11646340L);
//
//			if(true)
//				return buildResponse;




			//Mock for testing server
//						BuildResponse buildResponse = new BuildResponse(true, "Build has been started successfully", 11723376L, "RUNNING");
//						buildResponse.setProductId("4670");
//						buildResponse.setBuildType("FULLBUILD");
//						buildResponse.setCheckoutLabel("master");
//						buildResponse.setUrl("https://build.zohocorp.com/zoho/payout_server/webhost/master/Oct_16_2025_1/");
//
//						if(true)
//							return buildResponse;



			Map<String, ProductConfig> productConfigMap = getProductConfigMap();
			if(!productConfigMap.containsKey(productName))
			{
				String supportedRepos = String.join(", ", productConfigMap.keySet());
				return new BuildResponse("Invalid repo name provided: " + productName +
					"\n\nSupported repo names : " + supportedRepos);
			}

			ProductConfig config = productConfigMap.get(productName);
			String branchName = config.getBranch();
			String productId = config.getId();
			String userNames = "pradheep.rkd";

			BuildRequest buildRequest = createBuildRequest(branchName, productId, userNames);
			return callBuildApi(buildRequest);

		}
		catch(Exception e)
		{
			return new BuildResponse("Request failed. Please try again later. Error: " + e.getMessage());
		}
	}

	private void scheduleBuildWorkFlow(List<String> productNames)
	{
		try
		{
			BuildMonitorEntity monitor = buildMonitorService.createBuildMonitor(productNames);

			BuildProductEntity firstProduct = buildProductService.getNextPendingProduct(monitor.getId()).orElse(null);

			Map<String, Object> context = Map.of("monitorId", monitor.getId(), "productId", firstProduct.getId());

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

			JsonNode responseNode = objectMapper.readTree(response.getBody());

			if(responseNode.has("buildlogs") && responseNode.get("buildlogs").has("id"))
			{
				JsonNode buildlogs = responseNode.get("buildlogs");
				Long buildId = buildlogs.get("id").asLong();
				String status = buildlogs.has("status") ? buildlogs.get("status").asText() : "Unknown";
				String productId = buildlogs.has("product_id") ? buildlogs.get("product_id").asText() : null;
				String buildType = buildlogs.has("build_type") ? buildlogs.get("build_type").asText() : null;
				String checkoutLabel = buildlogs.has("checkout_label") ? buildlogs.get("checkout_label").asText() : null;
				String duration = buildlogs.has("duration") ? buildlogs.get("duration").asText() : null;
				String url = buildlogs.has("url") ? buildlogs.get("url").asText() : null;

				BuildResponse buildResponse = new BuildResponse(true, "Build has been started successfully", buildId, status);
				buildResponse.setProductId(productId);
				buildResponse.setBuildType(buildType);
				buildResponse.setCheckoutLabel(checkoutLabel);
				buildResponse.setDuration(duration);
				buildResponse.setUrl(url);
				buildResponse.setBuildLogId(buildId);

				return buildResponse;
			}

			return new BuildResponse(false, "Request failed. Please try again later.");

		}
		catch(HttpClientErrorException | HttpServerErrorException e)
		{
			return new BuildResponse(false, "API call failed: " + e.getMessage());
		}
		catch(Exception e)
		{
			return new BuildResponse(false, "Request failed. Please try again later.");
		}
	}

	public static class ProductConfig
	{
		private final String id;
		private final String channel;
		private final String branch;
		private boolean isServerRepo;
		private String buildUrl;

		public ProductConfig(String id, String channel, String branch, boolean isServerRepo, String buildUrl)
		{
			this.id = id;
			this.channel = channel;
			this.branch = branch;
			this.isServerRepo = isServerRepo;
			this.buildUrl = buildUrl;
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
	}

	public static class BuildResponse
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

	private static class BuildRequest
	{
		private BuildLog buildLog;

		public void setBuildLog(BuildLog buildLog)
		{
			this.buildLog = buildLog;
		}

		public BuildLog getBuildLog()
		{
			return buildLog;
		}
	}

	private static class BuildLog
	{
		private String checkout_label;
		private String status;
		private String report_needed;
		private String build_type;
		private String started_from;
		private String customize_info;
		private String success_mail;
		private String error_mail;
		private String security_report_needed;
		private String selected;
		private String product_id;
		private String instant_response;

		public String getCheckoutLabel()
		{
			return checkout_label;
		}

		public void setCheckoutLabel(String checkout_label)
		{
			this.checkout_label = checkout_label;
		}

		public String getStatus()
		{
			return status;
		}

		public void setStatus(String status)
		{
			this.status = status;
		}

		public String getReportNeeded()
		{
			return report_needed;
		}

		public void setReportNeeded(String report_needed)
		{
			this.report_needed = report_needed;
		}

		public String getBuildType()
		{
			return build_type;
		}

		public void setBuildType(String build_type)
		{
			this.build_type = build_type;
		}

		public String getStartedFrom()
		{
			return started_from;
		}

		public void setStartedFrom(String started_from)
		{
			this.started_from = started_from;
		}

		public String getCustomizeInfo()
		{
			return customize_info;
		}

		public void setCustomizeInfo(String customize_info)
		{
			this.customize_info = customize_info;
		}

		public String getSuccessMail()
		{
			return success_mail;
		}

		public void setSuccessMail(String success_mail)
		{
			this.success_mail = success_mail;
		}

		public String getErrorMail()
		{
			return error_mail;
		}

		public void setErrorMail(String error_mail)
		{
			this.error_mail = error_mail;
		}

		public String getSecurityReportNeeded()
		{
			return security_report_needed;
		}

		public void setSecurityReportNeeded(String security_report_needed)
		{
			this.security_report_needed = security_report_needed;
		}

		public String getSelected()
		{
			return selected;
		}

		public void setSelected(String selected)
		{
			this.selected = selected;
		}

		public String getProductId()
		{
			return product_id;
		}

		public void setProductId(String product_id)
		{
			this.product_id = product_id;
		}

		public String getInstantResponse()
		{
			return instant_response;
		}

		public void setInstantResponse(String instant_response)
		{
			this.instant_response = instant_response;
		}
	}
}
