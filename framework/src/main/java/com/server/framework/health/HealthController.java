package com.server.framework.health;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.server.framework.common.AppStarted;

@RestController
@RequestMapping("/_app")
public class HealthController
{

	// Called when ?tomcat param is present: GET /_app/health?tomcat
	// Raw Servlet 3.0 Async API — what Spring's SseEmitter uses internally.
	// startAsync() detaches the request from the Tomcat thread so it can be
	// served asynchronously without holding a container thread.
	@GetMapping(value = "/health", params = "tomcat")
	public void healthTomcat(HttpServletRequest request)
	{
		AsyncContext asyncContext = request.startAsync();
		asyncContext.setTimeout(30_000);

		asyncContext.start(() -> {
			HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
			response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
			response.setCharacterEncoding("UTF-8");
			try
			{
				PrintWriter writer = response.getWriter();
				String[] checks = { "DB connection", "Cache", "External services", "All systems operational" };
				for (int i = 0; i < checks.length; i++)
				{
					writer.write("id: " + (i + 1) + "\n");
					writer.write("event: health-check\n");
					writer.write("data: " + checks[i] + "\n\n");
					writer.flush(); // flush pushes the chunk to the client immediately
					Thread.sleep(1000);
				}
				asyncContext.complete(); // tells Tomcat the async cycle is done
			}
			catch (IOException | InterruptedException e)
			{
				asyncContext.complete();
			}
		});
	}

	// Called when ?chunked param is present: GET /_app/health?chunked
	// StreamingResponseBody writes directly to the response OutputStream.
	// Spring sets Transfer-Encoding: chunked automatically since Content-Length is unknown.
	@GetMapping(value = "/health", params = "chunked", produces = "application/x-ndjson")
	public ResponseEntity<StreamingResponseBody> healthChunked()
	{
		StreamingResponseBody body = outputStream -> {
			PrintWriter writer = new PrintWriter(outputStream);
			String[][] checks = {
				{ "db", "Checking DB connection" },
				{ "cache", "Checking cache" },
				{ "services", "Checking external services" },
				{ "summary", "All systems operational" }
			};
			for (int i = 0; i < checks.length; i++)
			{
				// Each line is a complete JSON object — parseable as it arrives
				writer.println("{\"step\":" + (i + 1) + ",\"check\":\"" + checks[i][0] + "\",\"message\":\"" + checks[i][1] + "\"}");
				writer.flush(); // flush forces the chunk to be sent immediately
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					break;
				}
			}
		};
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType("application/x-ndjson"))
			.body(body);
	}

	// Called when ?stream param is present: GET /_app/health?stream
	@GetMapping(value = "/health", params = "stream")
	public SseEmitter healthStream()
	{
		SseEmitter emitter = new SseEmitter(30_000L); // 30s timeout
		CompletableFuture.runAsync(() -> {
			try
			{
				String[] checks = { "Checking DB connection...", "Checking cache...", "Checking external services...", "All systems operational" };
				for (int i = 0; i < checks.length; i++)
				{
					emitter.send(SseEmitter.event()
						.id(String.valueOf(i + 1))
						.name("health-check")
						.data(checks[i]));
					Thread.sleep(1000);
				}
				emitter.send(SseEmitter.event()
					.name("done")
					.data("App started: " + AppStarted.APP_STARTED.toString()));
				emitter.complete();
			}
			catch (IOException | InterruptedException e)
			{
				emitter.completeWithError(e);
			}
		});
		return emitter;
	}

	@GetMapping("/health")
	public ResponseEntity<String> health() throws Exception
	{
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_PLAIN).body(AppStarted.APP_STARTED.toString());
	}

}
