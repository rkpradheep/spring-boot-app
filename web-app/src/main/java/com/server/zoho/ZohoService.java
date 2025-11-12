package com.server.zoho;

import com.server.framework.common.AppContextHolder;
import com.server.framework.common.AppProperties;
import com.server.framework.common.DateUtil;
import com.server.framework.common.CommonService;
import com.server.framework.error.AppException;
import com.server.framework.http.HttpResponse;
import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import com.server.framework.security.SecurityUtil;
import com.server.framework.service.ConfigurationService;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ZohoService
{
	private static final Logger LOGGER = Logger.getLogger(ZohoService.class.getName());
	private static final Map<String, String> DC_DOMAIN_MAPPING;
	static Map<String, Object> ZOHO_META_MAP;
	private static final Map<String, Long> TOKEN_HASH_EXPIRY_TIME = new ConcurrentHashMap<>();
	private static final Map<String, String> TOKEN_HASH_EMAIL_CACHE = new ConcurrentHashMap<>()
	{
		@Override public String get(Object tokenHash)
		{
			String email = super.get(tokenHash);
			if(StringUtils.isEmpty(email))
			{
				return null;
			}
			if(TOKEN_HASH_EXPIRY_TIME.get(tokenHash) < DateUtil.getCurrentTimeInMillis())
			{
				remove(tokenHash);
				TOKEN_HASH_EXPIRY_TIME.remove((String) tokenHash);
				return null;
			}
			return email;
		}

		@Override public String put(String tokenHash, String email)
		{
			TOKEN_HASH_EXPIRY_TIME.put(tokenHash, DateUtil.getCurrentTime().plusMinutes(30).toInstant().toEpochMilli());
			return super.put(tokenHash, email);
		}

		@Override public String computeIfAbsent(String tokenHash, Function<? super String, ? extends String> mappingFunction)
		{
			if(StringUtils.isNotEmpty(get(tokenHash)))
			{
				return get(tokenHash);
			}
			TOKEN_HASH_EXPIRY_TIME.put(tokenHash, DateUtil.getCurrentTime().plusMinutes(30).toInstant().toEpochMilli());
			return super.computeIfAbsent(tokenHash, mappingFunction);
		}
	};

	private ZohoService()
	{
		loadMeta();
	}

	public static String uploadBuild(String productName, String milestoneVersion, String dc, String region, String comments) throws Exception
	{
		return uploadBuild(productName, milestoneVersion, dc, region, "production", "", false, null);
	}

	public static String uploadBuild(String productName, String milestoneVersion, String dc, String region, String buildStage, boolean isPatchBuild, String patchBuildURL) throws Exception
	{
		return uploadBuild(productName, milestoneVersion, dc, region, buildStage, null, isPatchBuild, patchBuildURL);
	}

	public static String uploadBuild(String productName, String milestoneVersion, String dc, String region, String buildStage, String comments, boolean isPatchBuild, String patchBuildURL)
		throws Exception
	{
		JSONObject buildOptions = new JSONObject()
			.put("skip_continue", true)
			.put("iast_jar_needed", !region.equals("IN"));

		JSONArray notifyTo = new JSONArray().put("pradheep.rkd@zohocorp.com");

		ProductConfig productConfig = IntegService.getProductConfig(productName);
		String buildURL = isPatchBuild ? patchBuildURL.concat("/").concat(productConfig.getPatchBuildZipName()) : getBuildURLForMilestone(productName, milestoneVersion);

		String comment = StringUtils.defaultIfEmpty(comments, DateUtil.getFormattedCurrentTime("'Master Build' dd MMMM yyyy").toUpperCase());
		JSONObject sdBuildUpdatePayload = new JSONObject()
			.put("data_center", dc)
			.put("region", region)
			.put("deployment_mode", "live")
			.put("build_stage", buildStage)
			.put("build_url", buildURL)
			.put("is_grid_edited", false)
			.put("build_options", buildOptions)
			.put("notify_to", notifyTo)
			.put("comment", isPatchBuild ? "PATCH BUILD" : comment)
			.put("is_patch_update", isPatchBuild)
			.put("provision_type", "build_update");

		String sdBuildUpdateUrl = AppProperties.getProperty("zoho.sd.build.update.api.url");
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", getSDAccessToken());
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> requestEntity = new HttpEntity<>(sdBuildUpdatePayload.toString(), headers);

		return AppContextHolder.getBean(RestTemplate.class).postForObject(sdBuildUpdateUrl, requestEntity, String.class);
	}

	public static String getBuildURLForMilestone(String productName, String milestoneVersion)
	{
		ProductConfig productConfig = IntegService.getProductConfig(productName);
		return productConfig.getBuildUrl().replace("{0}", milestoneVersion);
	}

	private void loadMeta()
	{
		try
		{
			if(Objects.isNull(getClass().getClassLoader().getResource("zoho-properties.yml")))
			{
				LOGGER.info("zoho-properties.yml not found in classpath. Skipping product configuration load.");
				return;
			}
			ZOHO_META_MAP = new HashMap<>();

			try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream("zoho-properties.yml"))
			{
				Yaml yaml = new Yaml();
				Map<String, Object> data = yaml.load(inputStream);

				@SuppressWarnings("unchecked")
				List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("integ_products");

				Map<String, ProductConfig> productConfigMap = new HashMap<>();

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
						Integer order = config.get("order") != null ? (Integer) config.get("order") : -1;
						String gitlabUrl = config.get("gitlab_url") != null ? config.get("gitlab_url").toString() : null;
						String gitlabToken = config.get("gitlab_token") != null ? config.get("gitlab_token").toString() : null;
						String parentProduct = config.get("parent_product") != null ? config.get("parent_product").toString() : null;
						String gitlabIssueUrl = config.get("gitlab_issue_url") != null ? config.get("gitlab_issue_url").toString() : null;
						String patchBuildZipName = config.get("patch_build_zip_name") != null ? config.get("patch_build_zip_name").toString() : null;

						productConfigMap.put(productName, new ProductConfig(productName, productId, channelName, branchName, isServerRepo, buildUrl, order, gitlabUrl, gitlabToken, parentProduct, gitlabIssueUrl, patchBuildZipName));
					}
				}

				ZOHO_META_MAP.put("PRODUCT_CONFIGS", productConfigMap);
				LOGGER.info("Loaded " + productConfigMap.size() + " product configurations from YAML");

				ZOHO_META_MAP.put("BUILD_OWNERS", data.get("build_owners"));

				ZOHO_META_MAP.put("PAYOUT_PRODUCTS", data.get("payout_products"));
			}
		}
		catch(Exception e)
		{
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in loadProductConfiguration", e);
		}
	}

	public static void createOrSendMessageToThread(String urlString, Map<String, Object> context, String threadTitle, String message)
	{
		String messageID = (String) context.get("messageID");
		String gitlabIssueID = (String) context.get("gitlabIssueID");
		createOrSendMessageToThread(urlString, messageID, (String) context.get("serverProductName"), gitlabIssueID, threadTitle, message);
	}

	public static void createOrSendMessageToThread(String urlString, String messageID, String serverRepoName, String gitlabIssueID, String threadTitle, String message)
	{
		createOrSendMessageToThread(urlString, messageID, serverRepoName, gitlabIssueID, threadTitle, message, null);
	}

	public static void createOrSendMessageToThread(String urlString, String messageID, String serverRepoName, String gitlabIssueID, String threadTitle, String message, JSONObject reference)
	{
		try
		{
			if(StringUtils.isEmpty(messageID))
			{
				return;
			}
			URL url = new URL(urlString);

			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("POST");

			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestProperty("Content-Type", "application/json");

			JSONObject payload = new JSONObject();
			payload.put("text", message);
			payload.put("references", Objects.nonNull(reference) ? reference : JSONObject.NULL);
			payload.put("thread_message_id", messageID);
			payload.put("post_in_parent", false);
			payload.put("thread_title", threadTitle);
			payload.put("sync_message", true);
			payload.put("bot", new JSONObject().put("name", "Build Automation Bot").put("image", "data:image/png;base64,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"));

			httpURLConnection.getOutputStream().write(payload.toString().getBytes(StandardCharsets.UTF_8));

			CommonService.getResponse(httpURLConnection.getResponseCode() != 200 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());

			if(StringUtils.isEmpty(gitlabIssueID))
			{
				return;
			}

			if(!message.contains("GITLAB ISSUE CREATED"))
			{
				addCommentToIssue(serverRepoName, gitlabIssueID, message);
			}
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error in creating/updating GitLab issue", e);
		}
	}

	public static Object getMetaConfig(String key)
	{
		if(ZOHO_META_MAP == null)
		{
			return null;
		}
		return ZOHO_META_MAP.get(key);
	}

	static
	{

		Map<String, String> dcDomainMappingTmp = new HashMap<>();
		dcDomainMappingTmp.put("dev", "csez.zohocorpin.com");
		dcDomainMappingTmp.put("csez", "csez.zohocorpin.com");
		dcDomainMappingTmp.put("local", "localzoho.com");
		dcDomainMappingTmp.put("us", "zoho.com");
		dcDomainMappingTmp.put("in", "zoho.in");
		dcDomainMappingTmp.put("eu", "zoho.eu");
		dcDomainMappingTmp.put("au", "zoho.com.au");
		dcDomainMappingTmp.put("jp", "zoho.jp");
		dcDomainMappingTmp.put("ca", "zohocloud.ca");
		dcDomainMappingTmp.put("uk", "zoho.uk");

		DC_DOMAIN_MAPPING = Collections.unmodifiableMap(dcDomainMappingTmp);

	}

	public static void doAuthentication() throws Exception
	{
		if(Objects.isNull(SecurityUtil.getCurrentRequest()))
		{
			return;
		}
		if(SecurityUtil.getCurrentRequestURI().matches("/api/v1/zoho/(mark-as-paid-org|mark-as-test-org|org-count-increment)"))
		{
			return;
		}
		String currentUserEmail = getCurrentUserEmail();
		if(StringUtils.isNotEmpty(currentUserEmail))
		{
			String allowedUsers = AppContextHolder.getBean(ConfigurationService.class).getValue("zoho.critical.operation.allowed.users").orElse(AppProperties.getProperty("zoho.critical.operation.allowed.users"));
			boolean isValidUser = Arrays.stream(allowedUsers.split(",")).map(String::trim).anyMatch(currentUserEmail::equals);
			if(!isValidUser)
			{
				LOGGER.log(Level.SEVERE, "User " + currentUserEmail + " does not have access to perform this operation");
				throw new AppException("access_denied", "User " + currentUserEmail + " does not have access to perform this operation");
			}
			return;
		}

		String clientId = AppProperties.getProperty("zoho.auth.client.id");
		String redirectUri = AppProperties.getProperty("zoho.auth.redirect.uri");
		String scopes = AppProperties.getProperty("zoho.auth.scopes");

		URIBuilder builder = new URIBuilder(getDomainUrl("accounts", "/oauth/v2/auth", "in"));
		builder.addParameter("scope", scopes);
		builder.addParameter("client_id", clientId);
		builder.addParameter("response_type", "code");
		builder.addParameter("redirect_uri", redirectUri);
		builder.addParameter("prompt", "consent");
		builder.addParameter("access_type", "online");
		throw new AppException("reauth_required", "Authentication required", Map.of("auth_uri", builder.toString()));
	}

	public static Map<String, String> exchangeCodeForTokens(String authCode)
	{
		try
		{
			String clientId = AppProperties.getProperty("zoho.auth.client.id");
			String clientSecret = AppProperties.getProperty("zoho.auth.client.secret");
			String redirectUri = AppProperties.getProperty("zoho.auth.redirect.uri");

			if(authCode == null || authCode.isEmpty())
			{
				throw new IllegalArgumentException("Authorization code is required");
			}

			HttpContext context = new HttpContext("https://accounts.zoho.com/oauth/v2/token", "POST");
			context.setParam("grant_type", "authorization_code");
			context.setParam("client_id", clientId);
			context.setParam("client_secret", clientSecret);
			context.setParam("redirect_uri", redirectUri);
			context.setParam("code", authCode);

			JSONObject jsonResponse = HttpService.makeNetworkCallStatic(context).getJSONResponse();

			Map<String, String> tokens = new HashMap<>();
			tokens.put("access_token", jsonResponse.optString("access_token"));
			tokens.put("refresh_token", jsonResponse.optString("refresh_token"));
			tokens.put("expires_in", String.valueOf(jsonResponse.optInt("expires_in")));
			tokens.put("token_type", jsonResponse.optString("token_type"));
			tokens.put("status", "success");

			return tokens;

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error exchanging code for tokens", e);
		}

		Map<String, String> errorResponse = new HashMap<>();
		errorResponse.put("status", "error");
		errorResponse.put("message", "Failed to exchange code for tokens");
		return errorResponse;
	}

	public static String getCurrentUserEmail() throws Exception
	{
		if(Objects.isNull(SecurityUtil.getCurrentRequest()))
		{
			return "SCHEDULER";
		}
		String emailFromParam = SecurityUtil.getCurrentRequest().getParameter("user_email");
		if(StringUtils.isNotEmpty(emailFromParam))
		{
			return emailFromParam;
		}
		String zohoToken = SecurityUtil.getCookieValue("zoho_authenticated_token");
		if(StringUtils.isEmpty(zohoToken))
		{
			return null;
		}

		String zohoTokenDecrypted = CommonService.getAESDecryptedValue(zohoToken);
		String tokenHash = DigestUtils.sha256Hex(zohoTokenDecrypted);
		TOKEN_HASH_EMAIL_CACHE.computeIfAbsent(tokenHash, (k) -> {
			String url = ZohoService.getDomainUrl("accounts", "/oauth/user/info", "in");
			try
			{
				JSONObject response = HttpService.makeNetworkCallStatic(new HttpContext(url, HttpGet.METHOD_NAME).setHeadersMap(Map.of("Authorization", "Bearer ".concat(zohoTokenDecrypted)))).getJSONResponse();
				return response.optString("Email");
			}
			catch(IOException e)
			{
				throw new RuntimeException(e);
			}
		});

		return TOKEN_HASH_EMAIL_CACHE.get(tokenHash);
	}

	public static String getDomainUrl(String subDomain, String resourceUri, String dc)
	{
		return "https://" + subDomain + "." + DC_DOMAIN_MAPPING.get(dc) + resourceUri;
	}

	public static Set<String> getProductsForBuildInitiation(List<String> payoutProducts) throws Exception
	{
		HttpService httpService = AppContextHolder.getBean(HttpService.class);

		Set<String> productsForBuildInitiation = new HashSet<>();

		for(String product : payoutProducts)
		{
			ProductConfig productConfig = IntegService.getProductConfig(product);

			HttpContext context = new HttpContext(productConfig.getGitlabUrl(), HttpMethod.GET.name());
			context.setHeader("PRIVATE-TOKEN", productConfig.getGitlabToken());

			HttpResponse httpResponse = httpService.makeNetworkCall(context);
			JSONArray commits = new JSONArray(httpResponse.getStringResponse());
			if(!commits.isEmpty())
			{
				String url = "https://build.zohocorp.com/zoho/" + product + "/milestones/" + productConfig.getChannel();
				BuildResponse buildResponse = getBuildResponse(url);

				String commitSHAFromGitlab = commits.getJSONObject(0).getString("id");
				if(!StringUtils.equals(commitSHAFromGitlab, buildResponse.getCommitSHA()))
				{
					productsForBuildInitiation.add(product);
				}
			}
		}

		return productsForBuildInitiation;
	}

	public static JSONObject generatePayoutChangSetsFromIDC() throws Exception
	{
		String inURL = getSDINBuildURLFromSDAPI("payout_server", "production");
		return generatePayoutChangSetsForURL(inURL);
	}

	public static JSONObject generatePayoutChangSetsForURL(String buildURL) throws Exception
	{
		List<String> payoutProducts = (List<String>) ZohoService.getMetaConfig(("PAYOUT_PRODUCTS"));

		HttpService httpService = AppContextHolder.getBean(HttpService.class);

		JSONObject changeSets = new JSONObject();

		BuildResponse inBuildResponse = getBuildResponse(buildURL);
		String buildlogID = inBuildResponse.getBuildLogId() + "";

		JSONArray dependencies = getDependencies(buildlogID);

		JSONObject idcCommitDetails = new JSONObject();

		for(int i = 0; i < dependencies.length(); i++)
		{
			JSONObject dependency = dependencies.getJSONObject(i);
			String productid = dependency.get("dependency_product_id") + "";
			String url = dependency.getString("url");

			ProductConfig product = IntegService.getProductConfigForID(productid);
			if(Objects.isNull(product))
			{
				continue;
			}

			if(product.getProductName().equals("payout_common"))
			{
				String depsBuildlogID = dependency.get("dependency_id") + "";
				JSONArray commonDeps = getDependencies(depsBuildlogID);
				for(int j = 0; j < commonDeps.length(); j++)
				{
					JSONObject commonDep = commonDeps.getJSONObject(j);
					String commonDepsProductid = commonDep.get("dependency_product_id") + "";
					ProductConfig productConfig = IntegService.getProductConfigForID(commonDepsProductid);
					if(Objects.isNull(productConfig))
					{
						continue;
					}
					String productName = productConfig.getProductName();
					String commonDepURL = commonDep.getString("url");
					idcCommitDetails.put(productName, getBuildResponse(commonDepURL).getCommitSHA());
				}
				idcCommitDetails.put("payout_common", getBuildResponse(url).getCommitSHA());
			}
			else
			{
				idcCommitDetails.put(product.getProductName(), getBuildResponse(url).getCommitSHA());
			}
		}

		idcCommitDetails.put("payout_server", getBuildResponse(buildURL).getCommitSHA());

		for(String product : payoutProducts)
		{
			ProductConfig productConfig = IntegService.getProductConfig(product);

			HttpContext context = new HttpContext(productConfig.getGitlabUrl(), HttpMethod.GET.name());
			context.setHeader("PRIVATE-TOKEN", productConfig.getGitlabToken());

			HttpResponse httpResponse = httpService.makeNetworkCall(context);
			JSONArray commits = new JSONArray(httpResponse.getStringResponse());
			Set<String> mrsFetched = new HashSet<>();
			if(!commits.isEmpty())
			{
				String commitSHAFromGitlab = commits.getJSONObject(0).getString("id");
				String commitSHAFromIDC = idcCommitDetails.optString(product);
				if(!StringUtils.equals(commitSHAFromGitlab, commitSHAFromIDC))
				{
					JSONArray productChangeSet = new JSONArray();
					for(int i = 0; i < commits.length(); i++)
					{
						JSONObject commit = commits.getJSONObject(i);

						if(commit.getString("id").equals(commitSHAFromIDC))
						{
							break;
						}
						if(!commit.getString("title").startsWith("Merge branch "))
						{
							continue;
						}
						JSONObject commitInfo = new JSONObject();
						commitInfo.put("commitSHA", commit.getString("short_id"));
						commitInfo.put("commitMessage", commit.getString("message"));
						commitInfo.put("author", commit.getString("author_email"));
						commitInfo.put("webURL", commit.getString("web_url"));

						context = new HttpContext(productConfig.getGitlabUrl().replace("/commits", "/commits/" + commit.getString("short_id") + "/merge_requests"), HttpMethod.GET.name());
						context.setHeader("PRIVATE-TOKEN", productConfig.getGitlabToken());

						httpResponse = httpService.makeNetworkCall(context);
						JSONArray mrs = new JSONArray(httpResponse.getStringResponse());
						if(!mrs.isEmpty())
						{
							JSONObject mr = mrs.getJSONObject(0);
							commitInfo.put("webURL", mr.getString("web_url"));
							String authorName = mr.getJSONObject("author").getString("username");
							String authorEmail = authorName.contains(".") ? authorName + "@zohocorp.com" : commit.getString("author_email");
							commitInfo.put("author", authorEmail);
							commitInfo.put("mrTitle", mr.getString("title"));
							commitInfo.put("mergedDate", DateUtil.getFormattedTime(DateUtil.convertDateToMilliseconds(mr.getString("merged_at"), "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")));

							if(mrsFetched.contains(mr.getString("web_url")))
							{
								continue;
							}
							mrsFetched.add(mr.getString("web_url"));
						}

						productChangeSet.put(commitInfo);

					}
					if(!productChangeSet.isEmpty())
					{
						changeSets.put(product, productChangeSet);
					}
				}
			}
		}

		if(changeSets.isEmpty())
		{
			return new JSONObject();
		}

		return new JSONObject()
			.put("base_milestone", StringUtils.defaultIfEmpty(inBuildResponse.getReleaseVersion(), inBuildResponse.getCheckoutLabel()))
			.put("products_changesets", changeSets);

	}

	public static BuildResponse getBuildResponse(String url) throws Exception
	{
		HttpContext context = new HttpContext(AppProperties.getProperty("zoho.build.api.url").concat("/api/v1/buildlogs"), "GET");
		context.setParam("url", url);
		context.setHeader("PRIVATE-TOKEN", AppProperties.getProperty("zoho.build.api.token"));

		HttpResponse httpResponse = AppContextHolder.getBean(HttpService.class).makeNetworkCall(context);
		return AppContextHolder.getBean(IntegService.class).getBuildResponse(httpResponse.getStringResponse());
	}

	public static String getPatchBuildURL(String product, String stage) throws Exception
	{
		HttpContext context = new HttpContext(AppProperties.getProperty("zoho.build.api.url").concat("/api/v1/patch_build_details"), "GET");
		context.setParam("product_id", IntegService.getProductConfig(product).getId());
		context.setParam("stage", stage);
		context.setParam("grid_value", "IN2");
		context.setParam("product_name", "ZOHOPAYOUT");
		context.setHeader("PRIVATE-TOKEN", AppProperties.getProperty("zoho.build.api.token"));

		HttpResponse httpResponse = AppContextHolder.getBean(HttpService.class).makeNetworkCall(context);
		return AppContextHolder.getBean(IntegService.class).getBuildResponse(httpResponse.getStringResponse()).getPatchBuildUrl();
	}

	public static String getSDINBuildURLFromSDAPI(String product, String stage) throws Exception
	{
		try
		{
			HttpContext context = new HttpContext(AppProperties.getProperty("zoho.sd.build.status.api.url").replace("/{BUILD_ID}", ""), "GET");

			context.setParam("start_date", DateUtil.getFormattedTime(DateUtil.getCurrentTime().minusDays(10).toInstant().toEpochMilli(), "yyyy-MM-dd"));
			context.setParam("overall_status", "Completed");
			context.setParam("datacenter", "IN2");
			context.setParam("region", "IN");
			context.setParam("build_stage", "production");
			context.setParam("limit", 1);
			context.setParam("sort_order", "desc");
			context.setParam("sort_by", "zac_completed_at");
			context.setParam("process_type", "build_update");

			context.setHeader("AUTHORIZATION", ZohoService.getSDAccessToken());

			HttpResponse httpResponse = AppContextHolder.getBean(HttpService.class).makeNetworkCall(context);

			JSONObject buildDetails = new JSONObject(httpResponse.getStringResponse()).getJSONArray("details").getJSONObject(0)
				.getJSONObject("details").getJSONObject("build_details");

			return buildDetails.getJSONObject(buildDetails.keySet().stream().findFirst().get()).getString("url");
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error fetching SD IN build URL from SD API for product: " + product + " and stage: " + stage, e);
			return getPatchBuildURL(product, stage);
		}
	}

	public static Pair<String, String> getLatestMilestoneAndCommentForBuildUpload(String product, String stage)
	{
		try
		{
			String sdBuildOptionUrl = AppProperties.getProperty("zoho.sd.build.update.api.url").replace("/action/build_update", "/buildoptions");

			HttpContext context = new HttpContext(sdBuildOptionUrl, "GET");

			context.setParam("deployment_mode", "live");
			context.setParam("region", "IN");
			context.setParam("data_center", "IN2");
			context.setParam("provision_type", "build_update");
			context.setParam("build_stage", stage);

			context.setHeader("AUTHORIZATION", ZohoService.getSDAccessToken());

			HttpResponse httpResponse = AppContextHolder.getBean(HttpService.class).makeNetworkCall(context);

			JSONArray buildURLs = new JSONObject(httpResponse.getStringResponse()).getJSONArray("details").getJSONObject(0)
				.getJSONObject("build_options").getJSONObject("build_url").getJSONArray("urls");

			for(int i = 0; i < buildURLs.length(); i++)
			{
				JSONObject buildURLObj = buildURLs.getJSONObject(i);
				Pattern milestonePattern = Pattern.compile(".*/milestones/master/([\\w\\.]+)/.*");
				Matcher matcher = milestonePattern.matcher(buildURLObj.getString("url"));
				if(!buildURLObj.getBoolean("is_patch_url") && matcher.matches())
				{
					return new ImmutablePair<>(matcher.group(1), buildURLObj.getString("comment"));
				}
			}
			return null;
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error fetching SD IN build URL from SD API for product: " + product + " and stage: " + stage, e);
			return null;
		}
	}

	public static JSONArray getDependencies(String buildlogID) throws Exception
	{
		HttpContext context = new HttpContext(AppProperties.getProperty("zoho.build.api.url").concat("/api/v1/build_dependencies"), "GET");
		context.setParam("buildlog_id", buildlogID);
		context.setParam("verbose", true);
		context.setHeader("PRIVATE-TOKEN", AppProperties.getProperty("zoho.build.api.token"));

		HttpResponse httpResponse = AppContextHolder.getBean(HttpService.class).makeNetworkCall(context);
		return new JSONObject(httpResponse.getStringResponse()).getJSONArray("build_dependencies");
	}

	public static String createIssue(String product, String title)
	{
		try
		{
			HttpService httpService = AppContextHolder.getBean(HttpService.class);
			ProductConfig productConfig = IntegService.getProductConfig(product);

			HttpContext context = new HttpContext(productConfig.getGitlabIssueUrl(), HttpMethod.POST.name());
			context.setHeader("PRIVATE-TOKEN", productConfig.getGitlabToken());
			context.setParam("title", title);

			HttpResponse httpResponse = httpService.makeNetworkCall(context);

			return new JSONObject(httpResponse.getStringResponse()).getInt("iid") + "";
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error creating issue for product: " + product, e);
			return "";
		}
	}

	public static void addCommentToIssue(String product, String gitlabIssueID, String comment)
	{
		try
		{
			if(StringUtils.isEmpty(gitlabIssueID))
			{
				return;
			}

			comment = comment.replace("{@participants}", "");
			comment = comment.replace("Please start the testing", "");
			comment = comment.replaceAll("(\\*(.*)\\*)", "**$2**");
			comment = comment.replace("{@", "");
			comment = comment.replace("}", "");
			comment = comment.replace("\n", "<br/>");

			HttpService httpService = AppContextHolder.getBean(HttpService.class);
			ProductConfig productConfig = IntegService.getProductConfig(product);

			HttpContext context = new HttpContext(productConfig.getGitlabIssueUrl().concat("/".concat(gitlabIssueID).concat("/notes")), HttpMethod.POST.name());
			context.setHeader("PRIVATE-TOKEN", productConfig.getGitlabToken());
			context.setParam("body", comment);

			httpService.makeNetworkCall(context);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error adding comment for product: " + product, e);
		}
	}

	public static void editIssueDescription(String product, String gitlabIssueID, String description) throws Exception
	{
		try
		{
			if(StringUtils.isEmpty(gitlabIssueID))
			{
				return;
			}

			description = description.replaceAll("(\\*(.*)\\*)", "**$2**");
			description = description.replace("{@", "");
			description = description.replace("}", "");
			description = description.replace("{@participants}", "");
			description = description.replace("\n", "<br/>");

			HttpService httpService = AppContextHolder.getBean(HttpService.class);
			ProductConfig productConfig = IntegService.getProductConfig(product);

			HttpContext context = new HttpContext(productConfig.getGitlabIssueUrl().concat("/".concat(gitlabIssueID)), HttpMethod.GET.name());
			context.setHeader("PRIVATE-TOKEN", productConfig.getGitlabToken());

			HttpResponse httpResponse = httpService.makeNetworkCall(context);

			JSONObject responseJSON = new JSONObject(httpResponse.getStringResponse());

			if(StringUtils.isEmpty(responseJSON.optString("title")))
			{
				LOGGER.log(Level.SEVERE, "Invalid response : " + httpResponse.getStringResponse());
				return;
			}

			String existingDescription = responseJSON.optString("description", "");
			description = existingDescription + "<br/><br/><br/>" + "<b>" + DateUtil.getFormattedCurrentTime() + " : </b>" + description;

			context = new HttpContext(productConfig.getGitlabIssueUrl().concat("/".concat(gitlabIssueID)), HttpMethod.PUT.name());
			context.setHeader("PRIVATE-TOKEN", productConfig.getGitlabToken());
			context.setParam("description", description);

			httpService.makeNetworkCall(context);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error editing issue for product: " + product, e);
		}
	}

	public static String postMessageToChannel(String urlString, String message)
	{
		try
		{
			HttpContext httpContext = new HttpContext(urlString, "POST");

			JSONObject payload = new JSONObject();
			payload.put("text", message);
			payload.put("sync_message", true);
			payload.put("bot", new JSONObject().put("name", "Build Automation Bot").put("image", "data:image/png;base64,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"));
			httpContext.setBody(payload);
			String response = AppContextHolder.getBean(HttpService.class).makeNetworkCall(httpContext).getStringResponse();
			return new JSONObject(response).getString("message_id");
		}
		catch(Exception e)
		{
			return "";
		}
	}

	public static void postChangeSet(JSONObject changeSetResponse, String channelURL, Map<String, Object> cotext, boolean isIDC)
	{
		try
		{
			if(changeSetResponse.isEmpty())
			{
				return;
			}

			JSONObject productsChangeSetJSON = changeSetResponse.getJSONObject("products_changesets");

			Set<String> productsChangeSet = productsChangeSetJSON.keySet();
			String message = "Current IDC Milestone : *" + changeSetResponse.getString("base_milestone") + "*\n\n";
			message += "*Changesets Generated From IDC* : \n\n";

			if(!isIDC)
			{
				message = "Previous Build Milestone : *" + changeSetResponse.getString("base_milestone") + "*\n\n";
				message += "*Changesets Generated From Previous Build* : \n\n";
			}
			for(String product : productsChangeSet)
			{
				message += "Product : *" + product + "*\n";
				JSONArray changesDetailsArray = productsChangeSetJSON.getJSONArray(product);
				for(Object changesDetails : changesDetailsArray)
				{
					JSONObject changesDetailsJSON = (JSONObject) changesDetails;
					if(StringUtils.isNotEmpty("mrTitle"))
					{
						message += "MR : " + "[" + changesDetailsJSON.getString("mrTitle") + "](" + changesDetailsJSON.getString("webURL") + ")" + "\n";
					}
					else
					{
						message += "Commit Message : " + changesDetailsJSON.getString("commitMessage").replace("\n", "") + "\n";
					}
					message += "Author : {@" + changesDetailsJSON.getString("author") + "}\n";
					message += "Merged At : " + changesDetailsJSON.getString("mergedDate") + "\n";
					message += "\n";
				}

				message += "\n";
			}
			createOrSendMessageToThread(channelURL, cotext, "MASTER BUILD", message);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception while posting changeset", e);
		}
	}

	public static String getSDAccessToken() throws Exception
	{
		Optional<String> accessTokenOptional = AppContextHolder.getBean(ConfigurationService.class).getValue("SD_ACCESS_TOKEN");
		if(accessTokenOptional.isPresent())
		{
			return accessTokenOptional.get();
		}

		String token = AppProperties.getProperty("zoho.sd.build.refresh.token");
		String tokenRefreshURL = AppProperties.getProperty("zoho.sd.token.regenerate.url");

		HttpContext httpContext = new HttpContext(tokenRefreshURL, "POST");
		httpContext.setHeader("Authorization", token);
		httpContext.setParam("grant_type", "refresh_token");

		HttpResponse httpResponse = AppContextHolder.getBean(HttpService.class).makeNetworkCall(httpContext);
		String accessToken = new JSONObject(httpResponse.getStringResponse()).getJSONObject("data").getString("token");
		AppContextHolder.getBean(ConfigurationService.class).setValue("SD_ACCESS_TOKEN", accessToken, DateUtil.getCurrentTimeInMillis() + (DateUtil.ONE_MINUTE_IN_MILLISECOND * 50));

		return accessToken;
	}

	public static JSONObject getSDBuildStatus(String buildID) throws Exception
	{
		String sdBuildStatusFetchUrl = AppProperties.getProperty("zoho.sd.build.status.api.url").replace("{BUILD_ID}", buildID);

		HttpContext httpContext = new HttpContext(sdBuildStatusFetchUrl, "GET");
		httpContext.setHeader("Authorization", ZohoService.getSDAccessToken());

		HttpResponse httpResponse = AppContextHolder.getBean(HttpService.class).makeNetworkCall(httpContext);
		JSONObject buildDetails =  new JSONObject(httpResponse.getStringResponse()).getJSONArray("details").getJSONObject(0).getJSONObject("details").getJSONArray("build_details").getJSONObject(0);

		return new JSONObject()
			.put("status", buildDetails.getJSONObject("status"))
			.put("overall_status", buildDetails.getString("overall_status"))
			.put("region", buildDetails.getString("region"))
			.put("url", buildDetails.getString("url"));
	}
}

