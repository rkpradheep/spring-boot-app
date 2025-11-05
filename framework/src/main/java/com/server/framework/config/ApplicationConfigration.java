package com.server.framework.config;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.server.framework.common.AppProperties;
import com.server.framework.common.CustomThreadFactory;
import com.server.framework.common.PlaceholderResourceTransformer;
import com.server.framework.security.SecurityUtil;

@Configuration
public class ApplicationConfigration implements WebMvcConfigurer
{
	@Autowired
	private AppProperties appProperties;

	private static final Logger LOGGER = Logger.getLogger(ApplicationConfigration.class.getName());

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry)
	{
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.setCachePeriod(0);

		registry.addResourceHandler("/uploads/**")
			.addResourceLocations("file:" + SecurityUtil.getUploadsPath() + "/")
			.setCachePeriod(0);

		registry
			.addResourceHandler("/app.html")
			.addResourceLocations("classpath:/static/")
			.resourceChain(true)
			.addTransformer(new PlaceholderResourceTransformer());
	}

	@Bean
	public JavaMailSender getJavaMailSender()
	{
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("smtp.gmail.com");
		mailSender.setPort(587);

		String user = AppProperties.getProperty("mail.user");
		String password = AppProperties.getProperty("mail.password");
		if(StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password))
		{
			mailSender.setUsername(user);
			mailSender.setPassword(password);
		}
		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");

		return mailSender;
	}

	@Bean
	public TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer()
	{
		int corePoolSize = AppProperties.getIntProperty("tomcat.threadpool.corePoolSize", 10);
		int maxPoolSize = AppProperties.getIntProperty("tomcat.threadpool.maxPoolSize", 50);
		int keepAliveTime = AppProperties.getIntProperty("tomcat.threadpool.keepAliveSeconds", 60);
		int queueCapacity = AppProperties.getIntProperty("tomcat.threadpool.queueCapacity", 100);
		return protocolHandler -> {
			ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
				corePoolSize,
				maxPoolSize,
				keepAliveTime,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(queueCapacity)
			);
			customExecutor.setThreadFactory(new CustomThreadFactory("tomcat-http-"));

			protocolHandler.setExecutor(customExecutor);
		};
	}

	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer()
	{
		Integer httpPort = AppProperties.getIntProperty("server.http.port", null);
		if(Objects.isNull(httpPort))
		{
			return factory -> {};
		}

		LOGGER.log(Level.INFO, "HTTP Listening on port: {0}", httpPort + "");
		return factory -> {
			Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
			connector.setPort(httpPort);
			factory.addAdditionalTomcatConnectors(connector);
		};
	}

	@Bean
	public RestTemplate restTemplate()
	{
		RestTemplate restTemplate = new RestTemplate();

		restTemplate.setErrorHandler(new ResponseErrorHandler()
		{
			@Override
			public boolean hasError(ClientHttpResponse response)
			{
				return false;
			}

			@Override
			public void handleError(ClientHttpResponse response)
			{
			}
		});

		return restTemplate;
	}

	//Tomcat way
	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}
}