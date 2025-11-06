package com.server.zoho;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONObject;

import com.server.framework.common.AppProperties;
import com.server.framework.common.DateUtil;
import com.server.framework.error.AppException;
import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import com.server.framework.http.HttpResponse;

public class TaskEngineService
{
	private static final Map<String, String> OTJ_NAME_MAPPING;
	private static final Map<String, String> REPETITIVE_JOB_NAME_MAPPING;
	private static final Map<String, String> PERIODIC_REPETITION_NAME_MAPPING;
	private static final Map<String, String> CALENDER_REPETITION_NAME_MAPPING;
	private final String queueId;
	private final String taskEngineUrl;
	private final Map<String, String> headersMap;

	public TaskEngineService(String dc, String serviceId, String queueName) throws Exception
	{
		taskEngineUrl = ZohoService.getDomainUrl("taskengine", AppProperties.getProperty("taskengine.schedule.path"), dc);
		queueId = getQueueId(serviceId, queueName);

		headersMap = new HashMap<>();
		headersMap.put("q", queueId);
		headersMap.put("s", "y");
	}

	static Pair<Long, Long> getUserIdCustomerIdPair(JSONObject payload) throws Exception
	{
		if(StringUtils.isEmpty(payload.optString("zsid")))
		{
			return new ImmutablePair<>(payload.optLong("user_id", -1L), payload.optLong("customer_id", -1L));
		}

		JSONObject serviceCredentials = new JSONObject();

		String service = payload.getString("service").concat("-").concat(payload.getString("dc"));
		String ip = AppProperties.getProperty("db.$.ip".replace("$", service));
		String user = AppProperties.getProperty("db.$.user".replace("$", service));
		String password = AppProperties.getProperty("db.$.password".replace("$", service));
		String server = AppProperties.getProperty("db.$.server".replace("$", service));

		serviceCredentials.put("ip", ip);
		serviceCredentials.put("server", server);
		serviceCredentials.put("user", user);
		serviceCredentials.put("password", password);
		serviceCredentials.put("zsid", "admin");
		serviceCredentials.put("query", AppProperties.getProperty("sas.customerid.and.userid.fetch.query").replace("{0}", payload.getString("zsid")));

		Map output = (Map) ((List) SASController.handleSasRequestStatic(serviceCredentials).get("query_output")).get(0);
		if(StringUtils.equals((String) output.get("ID"), "<EMPTY>"))
		{
			throw new AppException("Invalid value provided for ZSID");
		}

		return new ImmutablePair<>(Long.parseLong(output.get("ID") + ""), Long.parseLong(output.get("CUSTOMERID") + ""));

	}

	static
	{
		Map<String, String> otjNameMappingTmp = new HashMap<>();
		otjNameMappingTmp.put("JOB_ID", "12");
		otjNameMappingTmp.put("ADMIN_STATUS", "74");
		otjNameMappingTmp.put("RETRY_SCHEDULE_ID", "92");
		otjNameMappingTmp.put("SCHEDULED_TIME", "62");
		otjNameMappingTmp.put("USER_ID", "112");
		otjNameMappingTmp.put("SCHEMAID", "42");
		otjNameMappingTmp.put("RETRY_SCHEDULE_NAME", "143");
		otjNameMappingTmp.put("CLASS_NAME", "133");
		otjNameMappingTmp.put("SCHEDULE_ID", "22");
		otjNameMappingTmp.put("IS_COMMISIONED", "84");
		otjNameMappingTmp.put("TRANSACTION_TIME", "51");
		otjNameMappingTmp.put("AUDIT_FLAG", "104");
		otjNameMappingTmp.put("TASK_ID", "122");

		OTJ_NAME_MAPPING = Collections.unmodifiableMap(otjNameMappingTmp);

		Map<String, String> repetitiveJobNameMappingTmp = new HashMap<>();
		repetitiveJobNameMappingTmp.put("JOB_ID", "12");
		repetitiveJobNameMappingTmp.put("ADMIN_STATUS", "74");
		repetitiveJobNameMappingTmp.put("RETRY_SCHEDULE_ID", "92");
		repetitiveJobNameMappingTmp.put("SCHEDULED_TIME", "62");
		repetitiveJobNameMappingTmp.put("IS_COMMON", "164");
		repetitiveJobNameMappingTmp.put("USER_ID", "172");
		repetitiveJobNameMappingTmp.put("SCHEMAID", "182");
		repetitiveJobNameMappingTmp.put("SCHEDULE_NAME", "153");
		repetitiveJobNameMappingTmp.put("RETRY_SCHEDULE_NAME", "193");
		repetitiveJobNameMappingTmp.put("CLASS_NAME", "133");
		repetitiveJobNameMappingTmp.put("SCHEDULE_ID", "142");
		repetitiveJobNameMappingTmp.put("IS_COMMISIONED", "84");
		repetitiveJobNameMappingTmp.put("TRANSACTION_TIME", "51");
		repetitiveJobNameMappingTmp.put("AUDIT_FLAG", "104");
		repetitiveJobNameMappingTmp.put("TASK_ID", "122");

		REPETITIVE_JOB_NAME_MAPPING = Collections.unmodifiableMap(repetitiveJobNameMappingTmp);

		Map<String, String> periodicRepetitionNameMappingTmp = new HashMap<>();
		periodicRepetitionNameMappingTmp.put("SCHEDULE_ID", "62");
		periodicRepetitionNameMappingTmp.put("SCHEDULE_MODE", "101");
		periodicRepetitionNameMappingTmp.put("IS_COMMON", "34");
		periodicRepetitionNameMappingTmp.put("USER_ID", "42");
		periodicRepetitionNameMappingTmp.put("END_DATE", "82");
		periodicRepetitionNameMappingTmp.put("START_DATE", "72");
		periodicRepetitionNameMappingTmp.put("TIME_PERIOD", "91");
		periodicRepetitionNameMappingTmp.put("SCHEDULE_NAME", "23");
		periodicRepetitionNameMappingTmp.put("SCHEMAID", "52");

		PERIODIC_REPETITION_NAME_MAPPING = Collections.unmodifiableMap(periodicRepetitionNameMappingTmp);

		Map<String, String> calenderRepetitionNameMappingTmp = new HashMap<>();
		calenderRepetitionNameMappingTmp.put("TIME_OF_DAY", "81");
		calenderRepetitionNameMappingTmp.put("FIRST_DAY_OF_WEEK", "171");
		calenderRepetitionNameMappingTmp.put("TZ", "143");
		calenderRepetitionNameMappingTmp.put("IS_COMMON", "34");
		calenderRepetitionNameMappingTmp.put("USER_ID", "42");
		calenderRepetitionNameMappingTmp.put("SKIP_FREQUENCY", "151");
		calenderRepetitionNameMappingTmp.put("SCHEDULE_NAME", "23");
		calenderRepetitionNameMappingTmp.put("SCHEMAID", "52");
		calenderRepetitionNameMappingTmp.put("SCHEDULE_ID", "62");
		calenderRepetitionNameMappingTmp.put("MONTH_OF_YEAR", "121");
		calenderRepetitionNameMappingTmp.put("USE_DATE_IN_REVERSE", "164");
		calenderRepetitionNameMappingTmp.put("REPEAT_FREQUENCY", "71");
		calenderRepetitionNameMappingTmp.put("YEAR_OF_DECADE", "131");
		calenderRepetitionNameMappingTmp.put("DATE_OF_MONTH", "111");
		calenderRepetitionNameMappingTmp.put("DAY_OF_WEEK", "91");
		calenderRepetitionNameMappingTmp.put("WEEK", "102");
		calenderRepetitionNameMappingTmp.put("RUN_ONCE", "184");

		CALENDER_REPETITION_NAME_MAPPING = Collections.unmodifiableMap(calenderRepetitionNameMappingTmp);

	}

	private static String getIdForOTJ(String name)
	{
		return OTJ_NAME_MAPPING.get(name);
	}

	private static String getIdForRepetitiveJob(String name)
	{
		return REPETITIVE_JOB_NAME_MAPPING.get(name);
	}

	private static String getNameForOTJ(String id)
	{
		return OTJ_NAME_MAPPING.entrySet().stream().filter(otjEntrySet -> StringUtils.equals(id, otjEntrySet.getValue())).findFirst().get().getKey();
	}

	private static String getNameForRepetitiveJob(String id)
	{
		return REPETITIVE_JOB_NAME_MAPPING.entrySet().stream().filter(otjEntrySet -> StringUtils.equals(id, otjEntrySet.getValue())).findFirst().get().getKey();
	}

	private static String getIdForRepetition(String name, boolean isPeriodic)
	{
		return isPeriodic ? PERIODIC_REPETITION_NAME_MAPPING.get(name) : CALENDER_REPETITION_NAME_MAPPING.get(name);
	}

	private static String getNameForRepetition(String id, boolean isPeriodic)
	{
		Map<String, String> repetitionNameMapping = isPeriodic ? PERIODIC_REPETITION_NAME_MAPPING : CALENDER_REPETITION_NAME_MAPPING;
		return repetitionNameMapping.entrySet().stream().filter(otjEntrySet -> StringUtils.equals(id, otjEntrySet.getValue())).findFirst().get().getKey();
	}

	public static TaskEngineService getInstance(String dc, String serviceId, String queueName) throws Exception
	{
		return new TaskEngineService(dc, serviceId, queueName);
	}

	public String getQueueId(String serviceId, String queueName) throws Exception
	{
		Map<String, String> headersMap = new HashMap<>();
		headersMap.put("queue", queueName);
		headersMap.put("service-id", serviceId);
		headersMap.put("opr", "get-queueid-of-queue");
		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(new HttpContext(taskEngineUrl.replace(AppProperties.getProperty("taskengine.schedule.path"), AppProperties.getProperty("taskengine.admin.path")), HttpPost.METHOD_NAME).setHeadersMap(headersMap));
		handleErrorResponse(httpResponse);

		return httpResponse.getResponseHeaders().get("queue-id");
	}

	public Map<String, Object> getJobDetails(long jobId, long customerId) throws Exception
	{
		JSONObject payload = new JSONObject()
			.put("id", "j10")
			.put("0", jobId)
			.put("1", customerId);

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(taskEngineUrl, HttpPost.METHOD_NAME, headersMap, payload);
		handleErrorResponse(httpResponse);

		JSONObject jobDetails = httpResponse.getJSONResponse();
		boolean isRepetitive = jobDetails.remove("md").equals("2");

		Function<String, String> getName = isRepetitive ? TaskEngineService::getNameForRepetitiveJob : TaskEngineService::getNameForOTJ;
		Map<String, Object> jobResponse = new HashMap<>();
		jobDetails.keySet().forEach(key -> jobResponse.put(getName.apply(key), StringUtils.equals(getName.apply(key), "SCHEDULED_TIME") ? DateUtil.getFormattedTime(jobDetails.getLong(key), DateUtil.DATE_WITH_TIME_FORMAT) : jobDetails.get(key)));

		return jobResponse;
	}

	public String addOrUpdateOTJ(List<Long> jobIdList, String className, String retryRepetition, Integer delaySeconds, long userId, long customerId) throws Exception
	{
		long jobId = (long) jobIdList.get(0);

		JSONObject jobDetails = new JSONObject()
			.put(getIdForOTJ("JOB_ID"), jobId)
			.put(getIdForOTJ("CLASS_NAME"), className)
			.put(getIdForOTJ("USER_ID"), userId)
			.put("md", "1")
			.put(getIdForOTJ("TRANSACTION_TIME"), -1)
			.put(getIdForOTJ("RETRY_SCHEDULE_NAME"), retryRepetition)
			.put(getIdForOTJ("SCHEMAID"), customerId);

		Iterator<String> iterator = jobDetails.keys();

		while(iterator.hasNext())
		{
			String key = iterator.next();
			if(jobDetails.get(key) instanceof String && StringUtils.isEmpty(jobDetails.getString(key)))
			{
				iterator.remove();
			}
		}
		String jobMethodId = "j3";
		try
		{
			getJobDetails(jobId, customerId);
			jobDetails.put(getIdForOTJ("SCHEDULED_TIME"), Objects.isNull(delaySeconds) ? null : DateUtil.getCurrentTimeInMillis() + (1000L * delaySeconds));
			jobMethodId = "j25";
		}
		catch(Exception e)
		{
			jobDetails.put(getIdForOTJ("SCHEDULED_TIME"), DateUtil.getCurrentTimeInMillis() + (1000L * ObjectUtils.defaultIfNull(delaySeconds, 0)));
		}

		JSONArray jobDetailsArray = new JSONArray();
		jobDetailsArray.put("1");
		jobIdList.forEach(jobIdObj -> jobDetailsArray.put(new JSONObject(jobDetails.toString()).put(getIdForOTJ("JOB_ID"), jobIdObj)));

		JSONObject payload = new JSONObject()
			.put("id", jobMethodId)
			.put("0", jobDetailsArray);

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(taskEngineUrl, HttpPost.METHOD_NAME, headersMap, payload);
		handleErrorResponse(httpResponse);

		return jobMethodId.equals("j25") ? "Job(s) updated successfully" : "Job(s) added successfully";
	}

	public String addOrUpdateRepetitiveJob(long jobId, String className, String repetition, String retryRepetition, Integer delaySeconds, long userId, long customerId) throws Exception
	{
		JSONObject jobDetails = new JSONObject()
			.put(getIdForRepetitiveJob("JOB_ID"), jobId)
			.put(getIdForRepetitiveJob("CLASS_NAME"), className)
			.put(getIdForRepetitiveJob("USER_ID"), userId)
			.put(getIdForRepetitiveJob("SCHEDULED_TIME"), Objects.nonNull(delaySeconds) ? DateUtil.getCurrentTimeInMillis() + (1000L * delaySeconds) : null)
			.put("md", "2")
			.put(getIdForRepetitiveJob("TRANSACTION_TIME"), -1)
			.put(getIdForRepetitiveJob("SCHEDULE_NAME"), repetition)
			.put(getIdForRepetitiveJob("RETRY_SCHEDULE_NAME"), retryRepetition)
			.put(getIdForRepetitiveJob("SCHEMAID"), customerId);

		Iterator<String> iterator = jobDetails.keys();

		while(iterator.hasNext())
		{
			String key = iterator.next();
			if(jobDetails.get(key) instanceof String && StringUtils.isEmpty(jobDetails.getString(key)))
			{
				iterator.remove();
			}
		}
		String jobMethodId = "j2";
		try
		{
			getJobDetails(jobId, customerId);
			jobMethodId = "j6";
		}
		catch(Exception ignored)
		{
		}

		JSONObject payload = new JSONObject()
			.put("id", jobMethodId)
			.put("0", jobDetails);

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(taskEngineUrl, HttpPost.METHOD_NAME, headersMap, payload);
		handleErrorResponse(httpResponse);

		return jobMethodId.equals("j6") ? "Repetitive Job updated successfully" : "Repetitive Job added successfully";
	}

	public String deleteJob(long jobId, long customerId) throws Exception
	{
		JSONObject payload = new JSONObject()
			.put("id", "j7")
			.put("0", jobId)
			.put("1", customerId);

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(taskEngineUrl, HttpPost.METHOD_NAME, headersMap, payload);
		handleErrorResponse(httpResponse);

		return "Job deleted successfully";
	}

	public Map<String, Object> getRepetitionDetails(String repetitionName, long userId, long customerId) throws Exception
	{
		JSONObject payload = new JSONObject()
			.put("id", "s8")
			.put("0", repetitionName)
			.put("1", userId)
			.put("2", customerId);

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(taskEngineUrl, HttpPost.METHOD_NAME, headersMap, payload);
		handleErrorResponse(httpResponse);

		JSONObject repetitionDetails = httpResponse.getJSONResponse();

		Map<String, Object> repetitionResponse = new HashMap<>();
		boolean isPeriodic = StringUtils.equals("3", repetitionDetails.getString("md"));
		repetitionDetails.remove("md");
		repetitionDetails.keySet().forEach(key -> repetitionResponse.put(getNameForRepetition(key, isPeriodic), repetitionDetails.get(key)));

		return repetitionResponse;
	}

	public String addOrUpdatePeriodicRepetition(String repetitionName, long userId, long customerId, Integer periodicity, boolean isCommon, Boolean isExecutionStartTimePolicy) throws Exception
	{
		JSONObject jobDetails = new JSONObject()
			.put(getIdForRepetition("TIME_PERIOD", true), periodicity)
			.put(getIdForRepetition("IS_COMMON", true), isCommon)
			.put(getIdForRepetition("SCHEDULE_NAME", true), repetitionName)
			.put("md", "3")
			.put(getIdForRepetition("USER_ID", true), userId)
			.put(getIdForRepetition("SCHEMAID", true), customerId);

		Iterator<String> iterator = jobDetails.keys();

		while(iterator.hasNext())
		{
			String key = iterator.next();
			if(jobDetails.get(key) instanceof String && StringUtils.isEmpty(jobDetails.getString(key)))
			{
				iterator.remove();
			}
		}
		String jobMethodId = "s1";
		Map<String, Object> repetitionDetails = null;
		try
		{
			repetitionDetails = getRepetitionDetails(repetitionName, userId, customerId);
			jobDetails.put(getIdForRepetition("SCHEDULE_MODE", true), Objects.nonNull(isExecutionStartTimePolicy) ? isExecutionStartTimePolicy ? 0 : 1 : null);
			jobMethodId = "s3";
		}
		catch(Exception e)
		{
			jobDetails.put(getIdForRepetition("SCHEDULE_MODE", true), ObjectUtils.defaultIfNull(isExecutionStartTimePolicy, false) ? 0 : 1);
		}
		if(Objects.nonNull(repetitionDetails) && Objects.nonNull(repetitionDetails.get("REPEAT_FREQUENCY")))
		{
			throw new AppException("Invalid request. Trying to update periodic repetition but the given repetition is calender repetition");
		}

		JSONObject payload = new JSONObject()
			.put("id", jobMethodId)
			.put("0", jobDetails);

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(taskEngineUrl, HttpPost.METHOD_NAME, headersMap, payload);
		handleErrorResponse(httpResponse);

		return jobMethodId.equals("s3") ? "Periodic Repetition updated successfully" : "Periodic Repetition added successfully";
	}

	public String addOrUpdateCalenderRepetition(String repetition, long userId, long customerId, boolean isCommon, String hourMinSec, String frequency, String dayOfWeek, String dateOfMonth) throws Exception
	{
		Map<String, Integer> frequencyMeta = new HashMap<>()
		{
			{
				put("daily", 0);
				put("weekly", 1);
				put("monthly", 2);
				//				put("yearly", 3);
			}
		};

		hourMinSec = Pattern.compile("(?<=^|:)([0-9])(?=(:|$))").matcher(hourMinSec).replaceAll("0$1");
		String[] hourMinSecArray = StringUtils.isNotEmpty(hourMinSec) ? hourMinSec.split(":") : null;
		if(Objects.nonNull(hourMinSecArray) && !Pattern.matches("[0-2][0-3]:[0-5][0-9]:[0-5][0-9]", hourMinSec))
		{
			throw new AppException("Invalid vale passed for time of day");
		}
		Integer timeOfDay = Objects.isNull(hourMinSecArray) ? null : Integer.parseInt(hourMinSecArray[0]) * 60 * 60 + Integer.parseInt(hourMinSecArray[1]) * 60 + Integer.parseInt(hourMinSecArray[2]);
		JSONObject jobDetails = new JSONObject()
			.put(getIdForRepetition("IS_COMMON", false), isCommon)
			.put(getIdForRepetition("SCHEDULE_NAME", false), repetition)
			.put("md", "4")
			.put(getIdForRepetition("TIME_OF_DAY", false), timeOfDay)
			.put(getIdForRepetition("USER_ID", false), userId)
			.put(getIdForRepetition("DAY_OF_WEEK", false), dayOfWeek)
			.put(getIdForRepetition("SCHEMAID", false), customerId)
			.put(getIdForRepetition("DATE_OF_MONTH", false), dateOfMonth);

		Iterator<String> iterator = jobDetails.keys();

		while(iterator.hasNext())
		{
			String key = iterator.next();
			if(jobDetails.get(key) instanceof String && StringUtils.isEmpty(jobDetails.getString(key)))
			{
				iterator.remove();
			}
		}
		String jobMethodId = "s2";
		Map<String, Object> repetitionDetails = null;
		try
		{
			repetitionDetails = getRepetitionDetails(repetition, userId, customerId);
			jobDetails.put(getIdForRepetition("REPEAT_FREQUENCY", false), repetitionDetails.get("REPEAT_FREQUENCY"));
			jobDetails.put(getIdForRepetition("TIME_OF_DAY", false), ObjectUtils.defaultIfNull(timeOfDay, repetitionDetails.get("TIME_OF_DAY")));
			jobDetails.put(getIdForRepetition("DAY_OF_WEEK", false), ObjectUtils.defaultIfNull(dayOfWeek, repetitionDetails.get("DAY_OF_WEEK")));
			jobDetails.put(getIdForRepetition("DATE_OF_MONTH", false), ObjectUtils.defaultIfNull(dayOfWeek, repetitionDetails.get("DATE_OF_MONTH")));
			jobMethodId = "s4";
		}
		catch(Exception ignored)
		{
			if(Objects.isNull(frequencyMeta.get(frequency)))
			{
				throw new AppException("Invalid value passed for Frequency");
			}
			if(Objects.isNull(timeOfDay))
			{
				throw new AppException("Invalid value passed for Time Of Day");
			}
			jobDetails.put(getIdForRepetition("REPEAT_FREQUENCY", false), frequencyMeta.get(frequency));
		}

		if(Objects.nonNull(repetitionDetails) && Objects.isNull(repetitionDetails.get("REPEAT_FREQUENCY")))
		{
			throw new AppException("Invalid request. Trying to update calender repetition but the given repetition is periodic repetition");
		}

		JSONObject payload = new JSONObject()
			.put("id", jobMethodId)
			.put("0", jobDetails);

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(taskEngineUrl, HttpPost.METHOD_NAME, headersMap, payload);
		handleErrorResponse(httpResponse);

		return jobMethodId.equals("s4") ? "Calender Repetition updated successfully" : "Calender Repetition added successfully";
	}

	public String deleteRepetition(String repetition, long userId, long customerId) throws Exception
	{
		JSONObject payload = new JSONObject()
			.put("id", "s5")
			.put("0", repetition)
			.put("1", userId)
			.put("2", customerId);

		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(taskEngineUrl, HttpPost.METHOD_NAME, headersMap, payload);
		handleErrorResponse(httpResponse);

		return "Repetition deleted successfully";
	}

	private void handleErrorResponse(HttpResponse httpResponse) throws Exception
	{
		if(httpResponse.getResponseHeaders().containsKey("excp"))
		{
			throw new AppException(httpResponse.getResponseHeaders().get("excp").replaceAll("((java|com)[\\.\\w]+: )(.*)", "$3"));
		}
	}

}
