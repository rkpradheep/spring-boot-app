package com.server.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.server.framework.common.ContextInitializer;
import com.server.framework.common.ContextInitialized;
import com.server.framework.common.ContextLoaded;

@SpringBootApplication(scanBasePackages = {"com.server"})
@EntityScan(basePackages = {"com.server"})
@EnableJpaRepositories(basePackages = {"com.server"})
public class WebApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(WebApplication.class);
		app.addInitializers(new ContextInitializer());
		app.addListeners(new ContextLoaded(), new ContextInitialized());
		app.run(args);
	}
}