package com.server.framework.common;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class AppReady implements ApplicationListener<ApplicationReadyEvent>
{
	private static final Logger LOGGER = Logger.getLogger(AppReady.class.getName());

	@Override public void onApplicationEvent(ApplicationReadyEvent event)
	{
		LOGGER.log(Level.INFO, "Application is ready to serve requests");
	}
}
