package com.server.framework.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.framework.error.AppException;
import com.server.framework.security.SecurityUtil;

@RestController
public class ShellCommandExecutionController
{
	private static final Logger LOGGER = Logger.getLogger(ShellCommandExecutionController.class.getName());

	@PostMapping("/api/v1/run")
	void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String command = request.getParameter("command");
		try
		{
			if(StringUtils.isBlank(request.getParameter("password")))
			{
				SecurityUtil.writerErrorResponse(response, "Password mandatory");
				return;
			}
			else if(!StringUtils.equals(request.getParameter("password"), AppProperties.getProperty("shell.execution.password")))
			{
				SecurityUtil.writerErrorResponse(response, "Incorrect password");
				return;
			}
			else if(StringUtils.isBlank(request.getParameter("command")))
			{
				SecurityUtil.writerErrorResponse(response, "Invalid command");
				return;
			}

			if(command.contains("takePhoto"))
			{
				takePhotoAndPostToBot(request, response);
			}
			else if(command.contains("takeVideo"))
			{
				new Thread(() -> takeVideoAndPostToBot(request, response)).start();
			}
			else if(command.contains("wakeUpAlert"))
			{
				executeCommand(request, response, new String[] {command.split(" ")[0], command.substring(command.indexOf(" ") + 1)});
			}
			else
			{
				executeCommand(request, response, new String[] {"bash", "-c", command.replace("${password}", AppProperties.getProperty("machine.password")).replace("${login.password}", AppProperties.getProperty("machine.login.password"))});
			}
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred ", e);
			response.getWriter().print("Failed :\n\n");
			response.getWriter().print(e.getMessage());
		}
	}

	public void takePhotoAndPostToBot(HttpServletRequest request, HttpServletResponse response) throws Exception
	{

		executeCommand(request, response, new String[] {"bash", "-c", "fswebcam --jpeg 100 myImage.jpeg"});

		String fileName = "myImage.jpeg";

		String boundary = "---" + Long.toHexString(DateUtil.getCurrentTimeInMillis());

		URL url = new URL("https://cliq.zoho.com/company/64396901/api/v2/bots/myserver/files?zapikey=" + AppProperties.getProperty("cliq.zapi.key"));

		HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
		httpURLConnection.setRequestMethod("POST");

		httpURLConnection.setDoOutput(true);
		httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		OutputStream outputStream = httpURLConnection.getOutputStream();
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

		writer.print("--" + boundary + "\r\n");
		writer.print("Content-Disposition: form-data; name=\"file\"; filename=\"myImage.jpeg\"\r\n");
		writer.print("Content-Type: image/jpeg\r\n\r\n");
		writer.flush();

		FileInputStream inputStream = new FileInputStream(fileName);
		byte[] buffer = new byte[4096];
		int bytesRead;
		while((bytesRead = inputStream.read(buffer)) != -1)
		{
			outputStream.write(buffer, 0, bytesRead);
		}
		outputStream.flush();
		inputStream.close();

		writer.append("\r\n--" + boundary + "--\r\n");
		writer.close();
		outputStream.close();

		LOGGER.info("API Response " + CommonService.getResponse(httpURLConnection.getInputStream()));
		//TODO: Alternative with advanced library to achieve the same

		//		String url = "https://cliq.zoho.com/company/64396901/api/v2/bots/myserver/files?zapikey=" + AppProperties.getProperty("cliq.zapi.key");
		//		File file = new File("myImage.jpeg");
		//
		//		HttpEntity entity = MultipartEntityBuilder.create()
		//			.addBinaryBody("file", file)
		//			.build();
		//
		//		HttpPost httpPost = new HttpPost(url);
		//		httpPost.setEntity(entity);
		//
		//		HttpClient httpClient = HttpClientBuilder.create().build();
		//		HttpResponse response = httpClient.execute(httpPost);
		//		writeResponse(getResponse(response.getEntity().getContent()), true);
	}

	public void takeVideoAndPostToBot(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			String seconds = request.getParameter("command").split(" ").length > 1 ? request.getParameter("command").split(" ")[1] : "5";

			executeCommand(request, response, new String[] {"bash", "-c", "takeVideo " + seconds});
			String fileName = "video.mp4";
			String filePath = CommonService.HOME_PATH + "/" + fileName;

			String boundary = "---" + Long.toHexString(DateUtil.getCurrentTimeInMillis());

			URL url = new URL("https://cliq.zoho.com/company/64396901/api/v2/bots/myserver/files?zapikey=" + AppProperties.getProperty("cliq.zapi.key"));

			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("POST");

			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

			OutputStream outputStream = httpURLConnection.getOutputStream();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

			writer.print("--" + boundary + "\r\n");
			writer.print("Content-Disposition: form-data; name=\"file\"; filename=\"video.mp4\"\r\n");
			writer.print("Content-Type: video/mp4\r\n\r\n");
			writer.flush();

			FileInputStream inputStream = new FileInputStream(filePath);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while((bytesRead = inputStream.read(buffer)) != -1)
			{
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
			inputStream.close();

			writer.append("\r\n--").append(boundary).append("--\r\n");
			writer.close();
			outputStream.close();

			LOGGER.info("API Response " + CommonService.getResponse(httpURLConnection.getInputStream()));

			new File(filePath).delete();
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception ", e);
		}
	}

	private void executeCommand(HttpServletRequest request, HttpServletResponse response, String[] cmdArray) throws Exception
	{
		response.getWriter().println(execute(cmdArray));
	}

	public void postCommandFailureMessage(String message, HttpServletRequest request, String command) throws Exception
	{
		CommonService.postMessageToBot("Proxy IP : *" + request.getRemoteAddr() + "*\n\n\nSource IP : *" + request.getHeader("X-FORWARDED-FOR") + "*\n\n\nCommand Executed :\n\n*" + command.replace(AppProperties.getProperty("machine.password"), "*********") + "*\n\n\n" + message);
	}

	public static String execute(String[] cmdArray) throws Exception
	{
		return execute(cmdArray, null);
	}

	public static String execute(String[] cmdArray, String workingDir) throws Exception
	{
		File[] dbusFiles = new File("/run/user/").listFiles();

		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		if(StringUtils.isNotEmpty(workingDir))
		{
			pb.directory(new File(workingDir));
		}
		Map<String, String> env = pb.environment();
		env.put("DISPLAY", ":1");

		if(Objects.nonNull(dbusFiles) && dbusFiles.length > 0)
		{
			env.put("DBUS_SESSION_BUS_ADDRESS", "unix:path=/run/user/" + dbusFiles[0].getName() + "/bus");
		}

		Process process = pb.start();
		int status = process.waitFor();

		StringWriter stringWriter = new StringWriter();

		IOUtils.copy(process.getErrorStream(), stringWriter);
		if(StringUtils.isEmpty(stringWriter.toString()))
		{
			IOUtils.copy(process.getInputStream(), stringWriter);
		}
		String output  = stringWriter.toString();

		if(status != 0)
		{
			throw new AppException("Command execution failed with status " + status + " and output : " + output);
		}

		return output;
	}
}
