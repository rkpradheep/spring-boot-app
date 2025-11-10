package com.server.framework.job;

import com.server.framework.common.DateUtil;
import com.server.framework.security.SecurityUtil;
import com.server.framework.service.JobService;
import com.server.framework.service.OTPService;
import com.server.framework.user.RoleEnum;
import com.server.framework.builder.ApiResponseBuilder;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController
{
	private static final Logger LOGGER = Logger.getLogger(JobController.class.getName());

	@Autowired
	private OTPService otpService;

	@Autowired
	private JobService jobService;

	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> listJobs()
	{
		try
		{
			Map<String, String> jobsMap = Arrays.stream(TaskEnum.values())
				.filter(taskEnum -> !taskEnum.getTaskName().equals(TaskEnum.REMINDER.getTaskName()) ||
					Objects.nonNull(SecurityUtil.getCurrentUser()) && SecurityUtil.getCurrentUser().getRoleType() == RoleEnum.ADMIN.getType())
				.collect(Collectors.toMap(TaskEnum::getTaskName, TaskEnum::getTaskDisplayName));

			Map<String, Object> response = ApiResponseBuilder.success("Jobs retrieved successfully", jobsMap);
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			Map<String, Object> response = ApiResponseBuilder.error("Failed to retrieve jobs: " + e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(response);
		}
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createJob(@RequestBody Map<String, Object> request)
	{
		try
		{
			JSONObject payload = new JSONObject(request);
			boolean isRecurring = payload.optBoolean("is_recurring", false);
			int dayInterval = payload.optInt("day_interval", -1);

			long millSeconds = payload.optLong("seconds", -1) != -1 ?
				(payload.getLong("seconds") * 1000L) + DateUtil.getCurrentTimeInMillis() : DateUtil.convertDateToMilliseconds(payload.getString("execution_date_time"), "yyyy-MM-dd HH:mm");

			if(millSeconds < -60 * 1000)
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error("Cannot schedule job for past time", 400));
			}

			if(StringUtils.equals("mail", payload.getString("task")))
			{
				if(StringUtils.isEmpty(payload.optString("otp_reference")))
				{
					return ResponseEntity.badRequest().body(ApiResponseBuilder.error("OTP is required", 400));
				}
				otpService.verifyOTP(payload.getString("otp_reference"), payload.getString("otp"));
			}

			Map<String, Object> data = new HashMap<>();

			Long jobID = jobService.scheduleJob(payload.getString("task"), payload.optString("data"), millSeconds, dayInterval, isRecurring);
			data.put("jobId", jobID);
			Map<String, Object> response = ApiResponseBuilder.success("Job has been scheduled successfully with ID " + jobID.toString(), data);
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred while creating job", e);
			Map<String, Object> response = ApiResponseBuilder.error(e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(response);
		}
	}

	@DeleteMapping("/scheduler/delete")
	public ResponseEntity<Map<String, Object>> deleteScheduledJob(@RequestParam String jobId)
	{
		try
		{
			if(StringUtils.isNotEmpty(jobId))
			{
				jobService.deleteJob(Long.parseLong(jobId));
				Map<String, Object> data = new HashMap<>();
				data.put("deletedJobId", jobId);
				Map<String, Object> response = ApiResponseBuilder.success("Deleted successfully", data);
				return ResponseEntity.ok(response);
			}
			else
			{
				return ResponseEntity.badRequest().body(ApiResponseBuilder.error("Invalid request", 400));
			}
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred while deleting job", e);
			Map<String, Object> response = ApiResponseBuilder.error("Failed to delete job", 500);
			return ResponseEntity.internalServerError().body(response);
		}
	}
}
