package com.server.framework.job;

import com.server.framework.common.AppProperties;
import com.server.framework.common.CommonService;
import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import com.server.framework.service.JobService;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class ReminderTask implements Task
{

	private static final Logger LOGGER = Logger.getLogger(ReminderTask.class.getName());
	private static final String CHAT_ID = "2244866302561890715";
	private static int EXECUTION_TRACKER = 0;

	@Autowired
	private JobService jobService;

	public void run(long jobID) throws Exception
	{
		LOGGER.log(Level.INFO, "Executing reminder job at {0}", new java.util.Date());

		String accessToken = generateAccessToken();
		JSONObject db = new JSONObject(getDBRecord(accessToken)).getJSONObject("object");

		JSONObject dbInvitationStatus = new JSONObject(db.getString("status"));

		List<String> pendingUser = dbInvitationStatus.keySet().stream()
			.filter(userID -> Arrays.asList("NR", "WT").contains(dbInvitationStatus.getString(userID)))
			.collect(Collectors.toList());

		if(pendingUser.isEmpty() || EXECUTION_TRACKER > 5)
		{
			LOGGER.log(Level.INFO, "Process skipped. EXECUTION_TRACKER value - {0} PENDING_USER SIZE - {1}",
				new Object[] {EXECUTION_TRACKER, pendingUser.size()});
			EXECUTION_TRACKER = 0;
			return;
		}

		EXECUTION_TRACKER++;

		jobService.scheduleJob(() -> {
			try
			{
				run(jobID);
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}, (5 * 60));
		String updateId = new JSONObject(db.getString("activityid")).optString("updateId", null);
		String recentActivityId = new JSONObject(db.getString("activityid")).getString("recentActivityId");

		JSONArray pendingUsers = new JSONArray();
		String message = "Are you ready now?\n";
		for(String userID : pendingUser)
		{
			pendingUsers.put(userID);
			message += "\n{@" + userID + "}";
		}

		HttpService.makeNetworkCallStatic(new HttpContext("https://cliq.zoho.com/api/v2/bots/callboty/alerts", HttpPost.METHOD_NAME)
			.setHeadersMap(Map.of("Authorization", "Bearer " + accessToken))
			.setBody(new JSONObject().put("text", "Are your ready now? Kindly update the invitation status.")
				.put("user_ids", pendingUsers)));

		String response = CommonService.postMessageToChat(CHAT_ID, message, recentActivityId, accessToken);
		String messageID = new JSONObject(response).getString("message_id").replaceAll("%20", "_");

		if(updateId != null)
			deleteMessage(accessToken, updateId);

		updateDBRecord(accessToken, recentActivityId, messageID);
	}

	private String generateAccessToken()
	{
		try
		{
			JSONObject jsonObject = CommonService.getZohoSecrets("us");

			StringBuilder stringBuilder = new StringBuilder("?")
				.append("client_id=").append(jsonObject.getString("client_id"))
				.append("&")
				.append("client_secret=").append(jsonObject.getString("client_secret"))
				.append("&")
				.append("refresh_token=").append(AppProperties.getProperty("remindertask.refresh_token"))
				.append("&")
				.append("grant_type=").append("refresh_token");

			URL url = new URL("https://accounts.zoho.com/oauth/v2/token".concat(stringBuilder.toString()));
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("POST");

			httpURLConnection.setDoOutput(true);
			String res = CommonService.getResponse(httpURLConnection.getResponseCode() != 200 ?
				httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
			return new JSONObject(res).getString("access_token");
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error generating access token", e);
			return null;
		}
	}

	private String getDBRecord(String accessToken)
	{
		try
		{
			URL url = new URL("https://cliq.zoho.com/company/64396901/api/v2/storages/invitation/records/1775998001292877737");
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("GET");

			httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

			return CommonService.getResponse(httpURLConnection.getResponseCode() != 200 ?
				httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error getting DB record", e);
			return null;
		}
	}

	private void deleteMessage(String accessToken, String messageID)
	{
		try
		{
			URL url = new URL("https://cliq.zoho.com/api/v2/chats/" + CHAT_ID + "/messages/" + messageID);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("DELETE");

			httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

			CommonService.getResponse(httpURLConnection.getResponseCode() != 200 ?
				httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
		}
		catch(Exception e)
		{
			LOGGER.log(Level.WARNING, "Error deleting message", e);
		}
	}

	private void updateDBRecord(String accessToken, String recentActivityId, String updateId)
	{
		try
		{
			JSONObject activity = new JSONObject();
			activity.put("recentActivityId", recentActivityId);
			activity.put("updateId", updateId);

			JSONObject data = new JSONObject();
			data.put("activityid", activity);
			JSONObject payLoad = new JSONObject();
			payLoad.put("values", data);

			URL url = new URL("https://cliq.zoho.com/company/64396901/api/v2/storages/invitation/records/1775998001292877737");
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("PUT");

			httpURLConnection.setDoOutput(true);

			httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

			httpURLConnection.getOutputStream().write(payLoad.toString().getBytes(StandardCharsets.UTF_8));

			CommonService.getResponse(httpURLConnection.getResponseCode() != 200 ?
				httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
		}
		catch(Exception e)
		{
			LOGGER.log(Level.WARNING, "Error updating DB record", e);
		}
	}
}
