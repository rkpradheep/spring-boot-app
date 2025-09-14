package com.server.framework.common;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ContextLoaded implements ApplicationListener<ApplicationPreparedEvent>
{
	private static final Logger LOGGER = Logger.getLogger(ContextLoaded.class.getName());

	@Override public void onApplicationEvent(ApplicationPreparedEvent event)
	{
		LOGGER.log(Level.INFO, "Application Context Loaded Successfully");
	}
}
