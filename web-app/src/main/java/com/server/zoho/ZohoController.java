package com.server.zoho;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.base64url.Base64Url;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.Inet4Address;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.server.framework.common.AppProperties;
import com.server.framework.common.CommonService;
import com.server.framework.error.AppException;
import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import com.server.framework.http.HttpResponse;
import com.server.framework.service.OAuthService;
import com.server.framework.builder.ApiResponseBuilder;

@RestController
@RequestMapping("/api/v1")
public class ZohoController
{

	private static final Hex HEX = new Hex();
	@Autowired
	private OAuthService oAuthService;

	@PostMapping("/zoho/isc")
	public ResponseEntity<Map<String, Object>> getISC(@RequestParam(name = "service") String service,
		@RequestParam(name = "dc") String dc,
		HttpServletRequest request)
	{
		try
		{
			String iscData = generateISCSignature(service, dc);
			Map<String, Object> response = ApiResponseBuilder.success("ISC data generated successfully", iscData);
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			Map<String, Object> response = ApiResponseBuilder.error("Failed to generate ISC data: " + e.getMessage(), 400);
			return ResponseEntity.badRequest().body(response);
		}
	}

	@PostMapping("/zoho/ear")
	public ResponseEntity<Map<String, Object>> doEARDecryption(@RequestBody Map<String, String> requestBody,
		HttpServletRequest request)
	{
		try
		{
			String service;
			String dc;
			String keyLabel;
			String cipherText;
			boolean isOEK;
			boolean isSearchable;

			service = requestBody.get("service");
			dc = requestBody.get("dc");
			keyLabel = requestBody.get("key_label");
			cipherText = requestBody.get("cipher_text");
			isOEK = Boolean.parseBoolean(requestBody.get("is_oek"));
			isSearchable = Objects.isNull(requestBody.get("is_searchable")) || Boolean.parseBoolean(requestBody.get("is_searchable"));

			String decryptedData = doEARDecryption(service, dc, keyLabel, cipherText, isOEK, isSearchable);
			Map<String, Object> response = ApiResponseBuilder.success("EAR data processed successfully", decryptedData);
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			Map<String, Object> response = ApiResponseBuilder.error("Failed to process EAR data: " + e.getMessage(), 400);
			return ResponseEntity.badRequest().body(response);
		}
	}

	@PostMapping("/zoho/jobs")
	public ResponseEntity<Map<String, Object>> handleZohoJobs(@RequestBody Map<String, String> requestBody, HttpServletRequest request) throws Exception
	{

		String service = requestBody.get("service");
		Pair<Long, Long> userIdCustomerIdPair = TaskEngineService.getUserIdCustomerIdPair(new JSONObject(requestBody));
		long userId = userIdCustomerIdPair.getKey();
		long customerId = userIdCustomerIdPair.getValue();

		String dc = requestBody.get("dc");
		String serviceId = AppProperties.getProperty("taskengine." + service + "-" + dc + ".service.id");
		String queueName = requestBody.get("thread_pool");
		String className = requestBody.get("class_name");
		Integer delaySeconds = StringUtils.isEmpty(requestBody.get("delay")) ? null : Integer.parseInt((requestBody.get("delay")));
		List<Long> jobIdList = Arrays.stream(requestBody.get("job_id").split(",")).map(String::trim).map(Long::parseLong).toList();
		long jobId = jobIdList.get(0);
		String retryRepetition = requestBody.get("retry_repetition");
		String repetition = requestBody.get("repetition");
		boolean isRepetitive = Boolean.parseBoolean(requestBody.get("is_repetitive"));

		String operation = requestBody.get("operation");

		if(StringUtils.equals("get", operation))
		{
			Map<String, Object> jobDetails = TaskEngineService.getInstance(dc, serviceId, queueName).getJobDetails(jobId, customerId);
			Map<String, Object> response = ApiResponseBuilder.success("Job details retrieved successfully", jobDetails);
			return ResponseEntity.ok(response);
		}

		ZohoService.doAuthentication();

		if(StringUtils.equals("add", operation))
		{
			String result = !isRepetitive ?
				TaskEngineService.getInstance(dc, serviceId, queueName).addOrUpdateOTJ(jobIdList, className, retryRepetition, delaySeconds, userId, customerId) :
				TaskEngineService.getInstance(dc, serviceId, queueName).addOrUpdateRepetitiveJob(jobId, className, repetition, retryRepetition, delaySeconds, userId, customerId);
			Map<String, Object> response = ApiResponseBuilder.success(result, null);
			return ResponseEntity.ok(response);
		}
		else if(StringUtils.equals("delete", operation))
		{
			Object result = TaskEngineService.getInstance(dc, serviceId, queueName).deleteJob(jobId, customerId);
			Map<String, Object> response = ApiResponseBuilder.success("Job deleted successfully", result);
			return ResponseEntity.ok(response);
		}

		Map<String, Object> response = ApiResponseBuilder.error("Invalid operation: " + operation, 400);
		return ResponseEntity.badRequest().body(response);
	}

	@PostMapping("/zoho/repetitions")
	public ResponseEntity<Object> handleRepetition(@RequestBody Map<String, String> requestBody,
		HttpServletRequest request) throws Exception
	{
		String service = requestBody.get("service");

		Pair<Long, Long> userIdCustomerIdPair = TaskEngineService.getUserIdCustomerIdPair(new JSONObject(requestBody));
		long userId = userIdCustomerIdPair.getKey();
		long customerId = userIdCustomerIdPair.getValue();

		String dc = requestBody.get("dc");
		String serviceId = AppProperties.getProperty("taskengine." + service + "-" + dc + ".service.id");
		String queueName = requestBody.get("thread_pool");
		String repetitionName = requestBody.get("repetition_name");
		if(StringUtils.isEmpty(repetitionName))
		{
			throw new AppException("Enter a valid value for repetition name");
		}

		boolean isCommon = Boolean.parseBoolean(requestBody.get("is_common"));
		if(!isCommon && userIdCustomerIdPair.getLeft() == -1L)
		{
			throw new AppException("Please provide valid DataSpace Name for non-common repetitions");
		}

		String operation = requestBody.get("operation");
		if(StringUtils.equals("get", operation))
		{
			return ResponseEntity.ok(Map.of("data", TaskEngineService.getInstance(dc, serviceId, queueName).getRepetitionDetails(repetitionName, userId, customerId)));
		}

		ZohoService.doAuthentication();
		if(StringUtils.equals("add_periodic", operation))
		{
			Integer periodicity = Integer.parseInt(StringUtils.defaultIfEmpty(requestBody.get("periodicity"), "-1"));
			periodicity = periodicity == -1 ? null : periodicity;
			Boolean isExecutionStartTimePolicy = Boolean.parseBoolean(requestBody.get("is_execution_start_time_policy"));
			return ResponseEntity.ok(Map.of("data", TaskEngineService.getInstance(dc, serviceId, queueName).addOrUpdatePeriodicRepetition(repetitionName, userId, customerId, periodicity, isCommon, isExecutionStartTimePolicy)));
		}
		else if(StringUtils.equals("add_calender", operation))
		{
			String hourMinSec = requestBody.get("time");
			String frequency = requestBody.get("frequency");
			String dayOfWeek = requestBody.get("day_of_week");
			return ResponseEntity.ok(Map.of("data", TaskEngineService.getInstance(dc, serviceId, queueName).addOrUpdateCalenderRepetition(repetitionName, userId, customerId, isCommon, hourMinSec, frequency, dayOfWeek)));
		}
		else if(StringUtils.equals("delete", operation))
		{
			return ResponseEntity.ok(Map.of("data", TaskEngineService.getInstance(dc, serviceId, queueName).deleteRepetition(repetitionName, userId, customerId)));
		}
		return null;
	}

	@GetMapping("/zoho/auth")
	public void authenticateZoho(@RequestParam(name = "code") String authCode, HttpServletResponse httpResponse) throws Exception
	{
		Map<String, Object> response = new HashMap<>();

		String message = "Authentication failed!";

		if(authCode != null && !authCode.isEmpty())
		{
			JSONObject tokenGeneratePayload = new JSONObject()
				.put("code", authCode)
				.put("client_id", AppProperties.getProperty("zoho.auth.client.id"))
				.put("client_secret", AppProperties.getProperty("zoho.auth.client.secret"))
				.put("redirect_uri", AppProperties.getProperty("zoho.auth.redirect.uri"))
				.put("url", ZohoService.getDomainUrl("accounts", "/oauth/v2/token", "in"));

			Map<String, Object> tokenResponse = oAuthService.generateTokens(tokenGeneratePayload.toMap());

			if((Boolean) tokenResponse.get("success"))
			{
				response.put("success", true);
				response.put("message", "Zoho authentication successful");
				response.put("access_token", tokenResponse.get("access_token"));
				response.put("refresh_token", tokenResponse.get("refresh_token"));
				response.put("expires_in", tokenResponse.get("expires_in"));

				String tokenHeader = "zoho_authenticated_token" + "="
					+ CommonService.getAESEncryptedValue((String) tokenResponse.get("access_token"))
					+ "; Path=/;"
					+ "Max-Age=1800;";
				httpResponse.setHeader("Set-Cookie", tokenHeader);
				message = "Authentication is success. Please try now.";

			}
		}

		String authHtml = "<html><body> " + message + " <script>setTimeout(function() {window.close();}, 2000);</script></body></script>";
		httpResponse.setContentType("text/html");
		httpResponse.getWriter().println(authHtml);
	}

	public static String generateISCSignature(String service, String dc) throws Exception
	{
		Set<String> useJwtServices = Arrays.stream(AppProperties.getProperty("security.services.use.jwt").split(",")).map(String::trim).collect(Collectors.toSet());
		if(useJwtServices.contains(service.concat("-").concat(dc)))
		{
			return generateJWTISCSignature(service, dc);
		}
		String encodedPrivateKey = AppProperties.getProperty("security.private.key.".concat(service).concat("-").concat(dc));
		byte[] privateKeyBytes = HEX.decode(encodedPrivateKey.getBytes());
		PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
		String currentTimeStr = String.valueOf(System.currentTimeMillis());
		Signature signature = Signature.getInstance("MD5withRSA");
		signature.initSign(privateKey);
		signature.update(currentTimeStr.getBytes());

		return AppProperties.getProperty("security." + service + ".iam.service.name") + "-" + currentTimeStr + "-" + new String(HEX.encode(signature.sign()));
	}

	public static String generateJWTISCSignature(String service, String dc) throws Exception
	{
		JwtClaims headerClaims = new JwtClaims();
		headerClaims.setClaim("typ", "JWT");
		headerClaims.setClaim("alg", "MD5withRSA");

		JwtClaims payloadClaims = new JwtClaims();
		payloadClaims.setIssuer(AppProperties.getProperty("security." + service + ".iam.service.name"));
		payloadClaims.setSubject(StringUtils.EMPTY);

		NumericDate numericDate = NumericDate.now();
		numericDate.setValue(numericDate.getValueInMillis());
		payloadClaims.setIssuedAt(numericDate);

		payloadClaims.setClaim("client_id", Inet4Address.getLocalHost().getHostAddress());

		String encodedHeader = Base64Url.encode(headerClaims.toJson().getBytes());
		String encodedPayload = Base64Url.encode(payloadClaims.toJson().getBytes());
		;

		String encodedPrivateKey = AppProperties.getProperty("security.private.key.".concat(service).concat("-").concat(dc));
		byte[] privateKeyBytes = HEX.decode(encodedPrivateKey.getBytes());
		PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
		Signature signature = Signature.getInstance("MD5withRSA");
		signature.initSign(privateKey);
		signature.update(encodedHeader.concat(".").concat(encodedPayload).getBytes());

		return encodedHeader + "." + encodedPayload + "." + Base64Url.encode(signature.sign());
	}

	public String doEARDecryption(String serviceName, String dc, String keyLabel, String cipherText, boolean isOEK, boolean isSearchable) throws Exception
	{
		String clientId = AppProperties.getProperty("ear." + serviceName + "-" + dc + "." + "client.id");
		String clientSecret = AppProperties.getProperty("ear." + serviceName + "-" + dc + "." + "client.secret");
		String refreshToken = AppProperties.getProperty("ear." + serviceName + "-" + dc + "." + "refresh.token");

		String tokenUrl = ZohoService.getDomainUrl("accounts", "/oauth/v2/token", dc);
		JSONObject tokenGeneratePayload = new JSONObject()
			.put("client_id", clientId)
			.put("client_secret", clientSecret)
			.put("refresh_token", refreshToken)
			.put("redirect_uri", "ear://")
			.put("url", tokenUrl);

		return doEARDecryption(keyLabel, cipherText, dc, isSearchable, isOEK, tokenGeneratePayload);
	}

	String doEARDecryption(String keyLabel, String cipherText, String dc, boolean isSearchable, boolean isOEK, JSONObject tokenGeneratePayload) throws Exception
	{

		String accessToken = new JSONObject(oAuthService.generateTokens(tokenGeneratePayload.toMap())).getString("access_token");

		String subDomain = dc.equals("csez") || dc.equals("local") ? "encryption" : "keystore";
		String resourceURI = isOEK ? "/kms/getDEK" : "/getKeyIv";
		String earURL = ZohoService.getDomainUrl(subDomain, resourceURI, dc);

		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put("operation", "0");
		if(isOEK)
		{
			parametersMap.put("org_id", keyLabel);
			parametersMap.put("kms_type", "1");
		}
		else
		{
			byte[] keyTokenHashedBytes = MessageDigest.getInstance("SHA-256").digest(keyLabel.getBytes());
			parametersMap.put("keytoken", new String(HEX.encode(keyTokenHashedBytes)));
		}

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(new HttpContext(earURL, "POST").setHeadersMap(Map.of("Authorization", "Bearer " + accessToken)).setParametersMap(parametersMap));

		JSONObject response = new JSONObject(httpResponse.getStringResponse());
		return doEARDecryption(cipherText, response.getString("key"), response.getString("iv"), isSearchable);
	}

	static String doEARDecryption(String cipherText, String key, String iv, boolean isSearchable) throws Exception
	{
		iv = isSearchable ? iv : cipherText.substring(0, 32);
		cipherText = isSearchable ? cipherText : cipherText.substring(32);

		SecretKeySpec secretKeySpec = new SecretKeySpec(HEX.decode(key.getBytes()), "AES");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(HEX.decode(iv.getBytes()));

		Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		decryptCipher.init(2, secretKeySpec, ivParameterSpec);

		byte[] decryptedDataBytes = decryptCipher.doFinal(HEX.decode(cipherText.getBytes()));
		return new String(decryptedDataBytes);
	}

}
