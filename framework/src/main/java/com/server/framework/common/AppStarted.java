package com.server.framework.common;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.server.framework.service.UserService;

@Component
public class AppStarted implements ApplicationListener<ApplicationStartedEvent>
{
	private static final Logger LOGGER = Logger.getLogger(AppStarted.class.getName());

	@Autowired
	private AppProperties appProperties;

	@Autowired
	private UserService userService;

	@Override public void onApplicationEvent(ApplicationStartedEvent event)
	{
		LOGGER.log(Level.INFO, "Application Started Successfully");
	}
}
