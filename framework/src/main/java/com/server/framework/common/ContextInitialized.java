package com.server.framework.common;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ContextInitialized implements ApplicationListener<ApplicationContextInitializedEvent>
{
	private static final Logger LOGGER = Logger.getLogger(ContextInitialized.class.getName());

	@Override public void onApplicationEvent(ApplicationContextInitializedEvent event)
	{
		LOGGER.log(Level.INFO, "Application Context Initialized Successfully");
	}
}
