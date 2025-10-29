package com.server.zoho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.framework.common.AppProperties;
import com.server.framework.common.DateUtil;
import com.server.framework.error.AppException;
import com.server.zoho.IntegService;
import com.server.zoho.ProductConfig;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class MilestoneService
{

	private static final Logger LOGGER = Logger.getLogger(MilestoneService.class.getName());

	@Autowired
	private RestTemplate restTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public MilestoneResult moveBuildToMilestone(Long buildId, String productName)
	{
		try
		{
			LOGGER.info("Starting milestone creation for build ID: " + buildId + ", product: " + productName);

			ProductConfig productConfig = IntegService.getProductConfig(productName);
			if(productConfig == null)
			{
				return new MilestoneResult(false, "Product mapping not found for: " + productName);
			}

			String productId = productConfig.getId();
			String branchName = productConfig.getBranch();

			LOGGER.info("Product ID: " + productId + ", Branch: " + branchName);

			String previousMilestone = getPreviousMilestoneVersion(productId, branchName);
			if(previousMilestone == null)
			{
				return new MilestoneResult(false, "Could not find previous milestone for product: " + productName);
			}

			LOGGER.info("Previous milestone: " + previousMilestone);

			String newMilestone = calculateNewMilestoneVersion(previousMilestone);
			LOGGER.info("New milestone: " + newMilestone);

			String finalMilestone = moveMilestoneToNewerBuild(buildId, newMilestone);

			return new MilestoneResult(true, "Milestone created successfully: " + finalMilestone, finalMilestone);

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error creating milestone: ", e);
			return new MilestoneResult(false, "Error creating milestone: " + e.getMessage());
		}
	}

	public ChannelMappingResult mapMilestoneToChannel(Long buildId, String milestoneVersion, String productName)
	{
		try
		{
			LOGGER.info("Starting channel mapping for build ID: " + buildId + ", milestone: " + milestoneVersion + ", product: " + productName);

			ProductConfig productConfig = IntegService.getProductConfig(productName);
			if(productConfig == null)
			{
				return new ChannelMappingResult(false, "Product mapping not found for: " + productName);
			}

			String productId = productConfig.getId();
			String channelName = productConfig.getChannel();

			LOGGER.info("Product ID: " + productId + ", Channel: " + channelName);

			Map<String, Object> payload = createChannelMappingPayload(productId, channelName, buildId);

			String apiUrl = AppProperties.getProperty("zoho.build.api.url") + "/api/v1/buildlinks";
			String apiToken = AppProperties.getProperty("zoho.build.api.token");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("PRIVATE-TOKEN", apiToken);

			HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

			LOGGER.info("Making API call to: " + apiUrl);
			LOGGER.info("Payload: " + objectMapper.writeValueAsString(payload));

			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

			LOGGER.info("Channel mapping API response: " + response.getBody());

			if(response.getStatusCode() == HttpStatus.CREATED)
			{
				JsonNode responseJson = objectMapper.readTree(response.getBody());

				if(responseJson.has("message") && !responseJson.get("message").isNull())
				{
					return new ChannelMappingResult(false, responseJson.get("message").asText());
				}

				if(responseJson.has("meta") && responseJson.get("meta").has("status"))
				{
					String status = responseJson.get("meta").get("status").asText();
					if("Success".equals(status))
					{
						String channelUrl = "";
						if(responseJson.has("buildlinks") && responseJson.get("buildlinks").has("url"))
						{
							channelUrl = responseJson.get("buildlinks").get("url").asText();
						}

						String message = responseJson.get("meta").get("message").asText().replaceAll("\\.", "") +
							" for " + milestoneVersion + "\n\n[Channel URL](" + channelUrl + ")";

						return new ChannelMappingResult(true, message, channelUrl);
					}
					else
					{
						return new ChannelMappingResult(false, responseJson.get("meta").get("message").asText());
					}
				}
			}

			return new ChannelMappingResult(false, "Unexpected response from channel mapping API");

		}
		catch(Exception e)
		{
			LOGGER.severe("Error mapping milestone to channel: " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in mapMilestoneToChannel", e);
			return new ChannelMappingResult(false, "Error mapping milestone to channel: " + e.getMessage());
		}
	}

	private String getPreviousMilestoneVersion(String productId, String branchName)
	{
		try
		{
			String apiUrl = AppProperties.getProperty("zoho.build.api.url") +
				"/api/v1/buildlogs?product_id=" + productId +
				"&availability=M&status=Success&per_page=1&checkout_label=" + branchName;
			String apiToken = AppProperties.getProperty("zoho.build.api.token");

			HttpHeaders headers = new HttpHeaders();
			headers.set("PRIVATE-TOKEN", apiToken);

			HttpEntity<String> requestEntity = new HttpEntity<>(headers);

			ResponseEntity<String> response = restTemplate.exchange(
				apiUrl, HttpMethod.GET, requestEntity, String.class);

			if(response.getStatusCode() == HttpStatus.OK)
			{
				JsonNode responseJson = objectMapper.readTree(response.getBody());

				if(responseJson.has("buildlogs") && responseJson.get("buildlogs").size() == 1)
				{
					return responseJson.get("buildlogs").get(0).get("release_version").asText();
				}
			}

			return null;

		}
		catch(Exception e)
		{
			LOGGER.severe("Error getting previous milestone version: " + e.getMessage());
			return null;
		}
	}

	private String calculateNewMilestoneVersion(String previousMilestone)
	{
		String pattern = "^([\\w]+)_(\\d+)\\.(\\d+)\\.(\\d+)(?:-(alpha|beta)(\\d+))?$";
		String product = previousMilestone.replaceAll(pattern, "$1");
		String major = previousMilestone.replaceAll(pattern, "$2");
		String minor = previousMilestone.replaceAll(pattern, "$3");
		String patch = previousMilestone.replaceAll(pattern, "$4");
		String alphaBeta = previousMilestone.replaceAll(pattern, "$5");
		String alphaBetaVersion = previousMilestone.replaceAll(pattern, "$6");

		LOGGER.info("Parsed milestone - Product: " + product + ", Major: " + major +
			", Minor: " + minor + ", Patch: " + patch + ", Alpha/Beta: " + alphaBeta + alphaBetaVersion);

		if(StringUtils.isNotEmpty(alphaBetaVersion))
		{
			return product + "_" + major + "." + minor + "." + patch + "-" + alphaBeta + (Integer.parseInt(alphaBetaVersion) + 1);
		}

		return product + "_" + major + "." + minor + "." + (Integer.parseInt(patch) + 1);
	}

	private String moveMilestoneToNewerBuild(Long buildLogId, String newMilestone) throws Exception
	{
		Map<String, Object> payload = new HashMap<>();
		Map<String, Object> movedDetail = new HashMap<>();

		movedDetail.put("location", newMilestone);
		movedDetail.put("comment", DateUtil.getFormattedCurrentTime("'Master Build' dd MMMM yyyy").toUpperCase());
		movedDetail.put("release_type", "MAJOR_OR_MINOR");
		movedDetail.put("buildlog_id", buildLogId);

		payload.put("movedDetail", movedDetail);

		String apiUrl = AppProperties.getProperty("zoho.build.api.url") + "/api/v1/moved_details";
		String apiToken = AppProperties.getProperty("zoho.build.api.token");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("PRIVATE-TOKEN", apiToken);

		HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

		ResponseEntity<String> response = restTemplate.exchange(
			apiUrl, HttpMethod.POST, requestEntity, String.class);

		LOGGER.info("Move Milestone Response : " + response.getBody());

		if(response.getStatusCode() == HttpStatus.CREATED)
		{
			JsonNode responseJson = objectMapper.readTree(response.getBody());

			if(responseJson.has("meta") && responseJson.get("meta").get("message").asText().contains("move Successfully"))
			{
				LOGGER.info("Milestone moved successfully");
				return newMilestone;
			}
		}

		LOGGER.warning("Failed to move milestone");
		throw new AppException("Failed to move milestone");

	}

	private Map<String, Object> createChannelMappingPayload(String productId, String channelName, Long buildId)
	{
		Map<String, Object> payload = new HashMap<>();
		Map<String, Object> movedDetails = new HashMap<>();

		movedDetails.put("remove_bld_mappings", false);
		movedDetails.put("is_active", true);
		movedDetails.put("product_id", productId);
		movedDetails.put("link", channelName);
		movedDetails.put("buildlog_id", buildId);

		payload.put("moved_details", movedDetails);

		return payload;
	}

	public static class MilestoneResult
	{
		private final boolean success;
		private final String message;
		private final String milestoneVersion;

		public MilestoneResult(boolean success, String message)
		{
			this.success = success;
			this.message = message;
			this.milestoneVersion = null;
		}

		public MilestoneResult(boolean success, String message, String milestoneVersion)
		{
			this.success = success;
			this.message = message;
			this.milestoneVersion = milestoneVersion;
		}

		public boolean isSuccess()
		{
			return success;
		}

		public String getMessage()
		{
			return message;
		}

		public String getMilestoneVersion()
		{
			return milestoneVersion;
		}
	}

	public static class ChannelMappingResult
	{
		private final boolean success;
		private final String message;
		private final String channelUrl;

		public ChannelMappingResult(boolean success, String message)
		{
			this.success = success;
			this.message = message;
			this.channelUrl = null;
		}

		public ChannelMappingResult(boolean success, String message, String channelUrl)
		{
			this.success = success;
			this.message = message;
			this.channelUrl = channelUrl;
		}

		public boolean isSuccess()
		{
			return success;
		}

		public String getMessage()
		{
			return message;
		}

		public String getChannelUrl()
		{
			return channelUrl;
		}
	}
}
