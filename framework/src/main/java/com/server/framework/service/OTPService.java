package com.server.framework.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.framework.error.AppException;

@Component
public class OTPService
{
	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private EmailService emailService;

	public void sendOTP(String to, String otp) throws AppException
	{
		String subject = "Your OTP Code";
		String htmlContent = String.format("""
			<html>
			<body>
			    <h2>Your OTP Code</h2>
			    <p>Your One-Time Password (OTP) is: <strong style="font-size: 18px; color: #007bff;">%s</strong></p>
			    <p>Please do not share this code with anyone.</p>
			    <br>
			    <p>If you didn't request this OTP, please ignore this email.</p>
			</body>
			</html>
			""", otp);

		emailService.sendHtmlEmail(null, to, subject, htmlContent);
	}

	public void verifyOTP(String reference, String otp) throws AppException
	{

		String storedOtpStr = configurationService.getValue(reference).get();
		if(!StringUtils.equals(storedOtpStr, otp))
		{
			throw new AppException("Invalid OTP reference or OTP");
		}
		configurationService.delete(reference);
	}
}
