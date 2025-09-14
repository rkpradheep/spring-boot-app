package com.server.framework.common;

import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.framework.builder.ApiResponseBuilder;
import com.server.framework.service.ConfigurationService;
import com.server.framework.service.OTPService;

@RestController
@RequestMapping("/api/v1")
public class OTPController
{

	@Autowired
	private OTPService otpService;

	@Autowired
	private ConfigurationService configurationService;

	@PostMapping("/initiate/otp")
	public ResponseEntity<Map<String, Object>> initiateOTP(@RequestParam(name = "email") String email)
	{
		int otp = new Random().nextInt(90000) + 10000;

		otpService.sendOTP(email, String.valueOf(otp));
		String otpReference = RandomStringUtils.randomAlphanumeric(10);
		configurationService.setValue(otpReference, String.valueOf(otp));

		Map<String, Object> response = ApiResponseBuilder.success("OTP has been initiated successfully", Map.of("otp_reference", otpReference));
		return ResponseEntity.ok(response);
	}

}
