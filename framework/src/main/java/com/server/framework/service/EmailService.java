package com.server.framework.service;

import com.server.framework.common.AppProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailService
{

	private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

	@Autowired
	private JavaMailSender mailSender;

	public boolean sendSimpleEmail(String to, String subject, String text)
	{
		return sendSimpleEmail(null, to, subject, text);
	}

	public boolean sendSimpleEmail(String from, String to, String subject, String text)
	{
		try
		{
			SimpleMailMessage message = new SimpleMailMessage();

			if(from != null && !from.isEmpty())
			{
				message.setFrom(from);
			}
			else
			{
				String defaultFrom = AppProperties.getProperty("mail.user");
				if(defaultFrom != null && !defaultFrom.isEmpty())
				{
					message.setFrom(defaultFrom);
				}
			}

			message.setTo(to);
			message.setSubject(subject);
			message.setText(text);

			mailSender.send(message);

			LOGGER.info("Simple email sent successfully to: " + to);
			return true;

		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to send simple email to: " + to, e);
			return false;
		}
	}

	public boolean sendHtmlEmail(String from, String to, String subject, String htmlContent)
	{
		try
		{
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			if(from != null && !from.isEmpty())
			{
				helper.setFrom(from);
			}
			else
			{
				String defaultFrom = AppProperties.getProperty("mail.user");
				if(defaultFrom != null && !defaultFrom.isEmpty())
				{
					helper.setFrom(defaultFrom);
				}
			}

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true); // true indicates HTML content

			mailSender.send(message);

			LOGGER.info("HTML email sent successfully to: " + to);
			return true;

		}
		catch(MessagingException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to send HTML email to: " + to, e);
			return false;
		}
	}

	public boolean sendEmailWithAttachment(String from, String to, String subject, String text,
		String attachmentPath, String attachmentName)
	{
		try
		{
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			if(from != null && !from.isEmpty())
			{
				helper.setFrom(from);
			}
			else
			{
				String defaultFrom = AppProperties.getProperty("mail.user");
				if(defaultFrom != null && !defaultFrom.isEmpty())
				{
					helper.setFrom(defaultFrom);
				}
			}

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(text);

			// Add attachment if provided
			if(attachmentPath != null && !attachmentPath.isEmpty())
			{
				helper.addAttachment(attachmentName != null ? attachmentName : "attachment",
					new java.io.File(attachmentPath));
			}

			mailSender.send(message);

			LOGGER.info("Email with attachment sent successfully to: " + to);
			return true;

		}
		catch(MessagingException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to send email with attachment to: " + to, e);
			return false;
		}
	}

}
