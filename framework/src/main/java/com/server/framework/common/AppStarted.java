package com.server.framework.common;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.server.framework.service.UserService;

@Component
public class AppStarted implements ApplicationListener<ApplicationStartedEvent>
{
	private static final Logger LOGGER = Logger.getLogger(AppStarted.class.getName());
	public static Boolean APP_STARTED = false;

	@Autowired
	private AppProperties appProperties;


	@Override public void onApplicationEvent(ApplicationStartedEvent event)
	{
		String username = AppProperties.getProperty("proxy.user");
		String password = AppProperties.getProperty("proxy.password");

		if(StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password))
		{
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password.toCharArray());
				}
			});
		}

		APP_STARTED = true;
		LOGGER.log(Level.INFO, "Application Started Successfully");
	}
}
