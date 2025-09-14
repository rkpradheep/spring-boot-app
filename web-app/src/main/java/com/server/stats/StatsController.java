package com.server.stats;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.concurrency.ConcurrencyController;
import com.server.framework.common.DateUtil;
import com.server.framework.error.AppException;
import com.server.framework.http.FormData;
import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import com.server.framework.http.HttpResponse;
import com.server.framework.security.SecurityUtil;
import com.server.framework.service.JobService;
import com.server.stats.meta.StatsMeta;
import com.server.framework.builder.ApiResponseBuilder;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController
{

	private static final List<String> RUNNING_STATS = new ArrayList<>();

	@Autowired
	private JobService jobService;

	private static final Logger LOGGER = Logger.getLogger(StatsController.class.getName());

	@Autowired
	private ConcurrencyController concurrencyController;

	@PostMapping
	ResponseEntity<Map<String, Object>> doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			Map<String, FormData> formDataMap = SecurityUtil.parseMultiPartFormData(request);
			FormData configurationFile = formDataMap.get("configuration_file");
			FormData configuration = formDataMap.get("configuration");
			FormData requestData = formDataMap.get("request_data");
			FormData requestDataText = formDataMap.get("request_data_text");
			if((Objects.isNull(requestData) && Objects.isNull(requestDataText)) || (Objects.isNull(configuration) && Objects.isNull(configurationFile)))
			{
				Map<String, Object> errorResponse = ApiResponseBuilder.error("Configuration or request data is missing", 400);
				return ResponseEntity.badRequest().body(errorResponse);
			}
			if(Objects.isNull(requestDataText) && !StringUtils.equals(requestData.getFileData().getContentType(), "text/csv"))
			{
				Map<String, Object> errorResponse = ApiResponseBuilder.error("Request data file is not in csv format", 400);
				return ResponseEntity.badRequest().body(errorResponse);
			}

			byte[] configurationFileBytes = Objects.nonNull(configurationFile) ? configurationFile.getFileData().getBytes() : configuration.getValue().getBytes();
			configurationFileBytes = new String(configurationFileBytes).replaceAll("&", "&amp;").getBytes();

			byte[] requestDataBytes = Objects.nonNull(requestDataText) ? requestDataText.getValue().getBytes() : requestData.getFileData().getBytes();
			String reqId = DigestUtils.sha256Hex(ByteBuffer.allocate(configurationFileBytes.length + requestDataBytes.length).put(configurationFileBytes).put(requestDataBytes).array());

			if(RUNNING_STATS.contains(reqId))
			{
				throw new AppException("Stats already running for this configuration with ReqId : " + reqId);
			}

			Reader requestDataReader = new InputStreamReader(new ByteArrayInputStream(requestDataBytes));

			StatsMeta statsMeta = StatsService.getStatsMeta(new ByteArrayInputStream(configurationFileBytes), requestDataReader, SecurityUtil.getUploadsPath() + "/" + reqId + ".csv");
			statsMeta.setRequestId(reqId);
			statsMeta.setRawResponseWriter(new FileWriter(SecurityUtil.getUploadsPath() + "/" + "RawResponse_" + reqId + ".txt"));
			statsMeta.setPlaceHolderWriter(new FileWriter(SecurityUtil.getUploadsPath() + "/" + "PlaceHolder_" + reqId + ".csv"));
			RUNNING_STATS.add(reqId);

			jobService.scheduleJob(() -> startStats(statsMeta), 1);

			File statsFile = new File(SecurityUtil.getUploadsPath() + "/" + reqId + ".csv");
			long starTime = DateUtil.getCurrentTimeInMillis();
			while(!statsFile.exists() && (DateUtil.getCurrentTimeInMillis() - starTime) < DateUtil.ONE_SECOND_IN_MILLISECOND * 5)
				;

			Map<String, Object> data = Map.of("request_id", reqId);
			Map<String, Object> apiResponse = ApiResponseBuilder.success("Stats request initiated successfully.", data);
			return ResponseEntity.ok(apiResponse);
		}
		catch(Exception e)
		{
			Map<String, Object> errorResponse = ApiResponseBuilder.error("Stats request failed: " + ExceptionUtils.getMessage(e), 500);
			LOGGER.log(Level.SEVERE, "Exception occurred", e);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	public void startStats(StatsMeta statsMeta) throws IOException
	{
		File outputFile = new File(statsMeta.getResponseFilePath());
		outputFile.createNewFile();
		int requestCount = 0;

		try(PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(outputFile))))
		{
			statsMeta.setResponseWriter(output);

			List<Map<String, String>> requestRowList = StatsService.getRequestRowList(statsMeta);

			statsMeta.getResponseWriter().println(statsMeta.getResponseHeaders());
			output.flush();

			List<Runnable> runnableList = new ArrayList<>();

			for(Map<String, String> requestDataRow : requestRowList)
			{
				try
				{
					statsMeta.incrementCount();
					requestCount++;

					int currentRequestNo = statsMeta.getRequestCount();
					runnableList.add(() -> makeCall(statsMeta, currentRequestNo, requestDataRow));

					if(requestCount >= statsMeta.getRequestBatchSize() || statsMeta.getRequestCount() >= requestRowList.size())
					{
						if(statsMeta.isDisableParallelCalls())
						{
							runnableList.forEach(Runnable::run);
						}
						else
						{
							concurrencyController.executeAsynchronously(runnableList);
						}

						runnableList.clear();
						if(statsMeta.getRequestCount() >= requestRowList.size())
						{
							break;
						}
						long waitTime = DateUtil.getCurrentTimeInMillis() + (1000L * statsMeta.getRequestIntervalSeconds());
						while(waitTime > DateUtil.getCurrentTimeInMillis())
							;
						requestCount = 0;
					}
				}
				catch(Exception e)
				{
					LOGGER.log(Level.INFO, "Exception for Request Count : " + statsMeta.getRequestCount(), e);
				}
			}

		}
		catch(Exception e)
		{
			LOGGER.severe("Exception in processStatsRequest: " + e.getMessage());
			LOGGER.log(Level.SEVERE, "Exception in processStatsRequest", e);
		}
		finally
		{

			RUNNING_STATS.remove(statsMeta.getRequestId());
			File desFile = new File(outputFile.getAbsolutePath().replace("_inprocess", StringUtils.EMPTY));
			outputFile.renameTo(desFile);
			LOGGER.log(Level.INFO, "Stats completed for ReqId : {0}", statsMeta.getRequestId());

			statsMeta.getRawResponseWriter().close();
			statsMeta.getResponseWriter().close();
			statsMeta.getPlaceHolderWriter().close();
		}
	}

	static void makeCall(StatsMeta statsMeta, int requestCount, Map<String, String> requestDataRow)
	{
		try
		{
			ImmutableTriple<String, Map<String, String>, JSONObject> placeHolderTriple = StatsService.handlePlaceholder(statsMeta, requestCount, requestDataRow);

			String response = connect(statsMeta, placeHolderTriple.getLeft(), placeHolderTriple.getMiddle(), placeHolderTriple.getRight());

			handleResponse(statsMeta, placeHolderTriple, response, requestCount, requestDataRow);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.INFO, "Exception for Request Count : " + requestCount, e);
		}
	}

	static void handleResponse(StatsMeta statsMeta, Triple<String, Map<String, String>, JSONObject> placeHolderTriple, String response, int requestCount, Map<String, String> requestDataRow)
		throws Exception
	{
		StringBuilder rowBuilder = new StringBuilder();
		for(String responseColumnName : statsMeta.getResponseColumnNames())
		{
			String columnValue = StatsService.getColumnValue(statsMeta, responseColumnName, placeHolderTriple, response, requestCount, requestDataRow);
			rowBuilder.append("\"").append(StringUtils.replace(columnValue, "\"", "\"\"")).append("\"").append(",");
		}
		synchronized(statsMeta.getResponseWriter())
		{
			statsMeta.getResponseWriter().println(rowBuilder.toString().replaceAll(",$", ""));
			statsMeta.getResponseWriter().flush();

			statsMeta.getRawResponseWriter().write("Request No : " + requestCount + "\n");
			statsMeta.getRawResponseWriter().write(response);
			statsMeta.getRawResponseWriter().write("\n\n\n\n");
			statsMeta.getRawResponseWriter().flush();
		}
	}

	public static String connect(StatsMeta statsMeta, String connectionUrl, Map<String, String> params, JSONObject jsonBody) throws Exception
	{
		HttpResponse httpResponse = HttpService.makeNetworkCallStatic(new HttpContext(connectionUrl, statsMeta.getMethod()).setHeadersMap(statsMeta.getRequestHeaders()).setParametersMap(params).setBody(jsonBody));
		return httpResponse.getStringResponse();
	}
}
