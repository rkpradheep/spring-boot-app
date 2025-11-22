package com.server.zoho;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.server.framework.common.AppContextHolder;
import com.server.framework.common.AppProperties;
import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.common.ShellCommandExecutionController;
import com.server.framework.entity.ConfigurationEntity;
import com.server.framework.http.HttpContext;
import com.server.framework.http.HttpResponse;
import com.server.framework.http.HttpService;
import com.server.framework.service.ConfigurationService;

public class DDDiffService
{
	private static final Logger LOGGER = Logger.getLogger(DDDiffService.class.getName());

	private static final ConfigurationService CONFIGURATION_SERVICE = AppContextHolder.getBean(ConfigurationService.class);

	public static void downloadBuildsAndGenerateDiff(String oldBuildUrl, String newBuildUrl, Path tempDir, String referenceID) throws Exception
	{
		try
		{
			ConfigurationEntity configurationEntity = CONFIGURATION_SERVICE.get(Long.parseLong(referenceID)).get();

			setValue(configurationEntity.getCKey(), "DOWNLOADING_OLD_BUILD");

			HttpContext httpContext = new HttpContext(oldBuildUrl, "GET");
			httpContext.setHeader("Authorization", "Basic " + AppProperties.getProperty("zoho.build.download.basic.header.token"));
			HttpResponse httpResponse = HttpService.makeNetworkCallStatic(httpContext);

			InputStream inputStream = httpResponse.getInputStream();

			Path oldBuildPath = tempDir.resolve("old_build.zip");
			Files.copy(inputStream, oldBuildPath);
			inputStream.close();

			if(StringUtils.isNotEmpty(newBuildUrl))
			{
				setValue(configurationEntity.getCKey(), "DOWNLOADING_NEW_BUILD");

				httpContext = new HttpContext(newBuildUrl, "GET");
				httpContext.setHeader("Authorization", "Basic " + AppProperties.getProperty("zoho.build.download.basic.header.token"));
				httpResponse = HttpService.makeNetworkCallStatic(httpContext);

				inputStream = httpResponse.getInputStream();
				Path newBuildPath = tempDir.resolve("new_build.zip");
				Files.copy(inputStream, newBuildPath);
				inputStream.close();
			}
			generateDiffReport(referenceID);

			setValue(configurationEntity.getCKey(), "COPYING_DIFF_REPORT");

			String status = "COMPLETED";
			String diff;
			try(BufferedReader bufferedReader = new BufferedReader(new FileReader(tempDir.resolve("old_build/AdventNet/Sas/tomcat/webapps/ROOT/WEB-INF/isu/dd-changes.sql").toString())))
			{
				diff = bufferedReader.lines().collect(Collectors.joining("\n"));
			}
			CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), new JSONObject().put("status", status).put("diff", diff).toString(), DateUtil.getCurrentTimeInMillis() + (DateUtil.ONE_DAY_IN_MILLISECOND * 7));
		}
		catch(Exception e)
		{
			ConfigurationEntity configurationEntity = CONFIGURATION_SERVICE.get(Long.parseLong(referenceID)).get();
			JSONObject statusObject = new JSONObject(configurationEntity.getCValue());
			setValue(configurationEntity.getCKey(), statusObject.getString("status").concat("_FAILED"));
			throw e;
		}
		finally
		{
			CommonService.cleanAndDeleteDirectory(tempDir);
		}
	}

	public static void generateDiffReport(String referenceID) throws Exception
	{
		ConfigurationEntity configurationEntity = CONFIGURATION_SERVICE.get(Long.parseLong(referenceID)).get();

		Path tempDir = Path.of(configurationEntity.getCKey());

		setValue(configurationEntity.getCKey(), "UNZIPPING_OLD_BUILD");
		CommonService.unzip(tempDir.resolve("old_build.zip").toString(), tempDir.resolve("old_build").toString());

		setValue(configurationEntity.getCKey(), "UNZIPPING_NEW_BUILD");
		CommonService.unzip(tempDir.resolve("new_build.zip").toString(), tempDir.resolve("new_build").toString());

		setValue(configurationEntity.getCKey(), "EXTRACTING_OLD_BUILD_ROOT_WAR");
		CommonService.unzip(tempDir.resolve("old_build/AdventNet/Sas/tomcat/webapps/ROOT.war").toString(), tempDir.resolve("old_build/AdventNet/Sas/tomcat/webapps/ROOT").toString());

		setValue(configurationEntity.getCKey(), "EXTRACTING_NEW_BUILD_ROOT_WAR");
		CommonService.unzip(tempDir.resolve("new_build/AdventNet/Sas/tomcat/webapps/ROOT.war").toString(), tempDir.resolve("new_build/AdventNet/Sas/tomcat/webapps/ROOT").toString());


		String content = Files.readString(tempDir.resolve("old_build/AdventNet/Sas/bin/sqlCreation.sh"), StandardCharsets.UTF_8);
		content = content.replace("generate.destructive.changes=false", "generate.destructive.changes=true");

		Files.writeString(tempDir.resolve("old_build/AdventNet/Sas/bin/sqlCreation.sh"), content, StandardCharsets.UTF_8);

		setValue(configurationEntity.getCKey(), "CHANGING_SQLCREATION_FILE_PERMISSION");

		ShellCommandExecutionController.execute(new String[]{"bash", "-c",
			"chmod +x " + tempDir.resolve("old_build/AdventNet/Sas/bin/sqlCreation.sh")});

		setValue(configurationEntity.getCKey(), "GENERATING_DIFF_REPORT");

		String response = ShellCommandExecutionController.execute(new String[]{"bash", "-c",
			"sh sqlCreation.sh " + tempDir.resolve("old_build/AdventNet/Sas/tomcat/webapps/ROOT/WEB-INF") + "/" + " " + tempDir.resolve("new_build/AdventNet/Sas/tomcat/webapps/ROOT/WEB-INF") + "/" + " -Dgenerate.destructive.changes=true"
		}, tempDir.resolve("old_build/AdventNet/Sas/bin").toString());

		LOGGER.log(Level.INFO, "DD diff generation response : {0}", response);

	}
	
	private static void setValue(String key, String value)
	{
		CONFIGURATION_SERVICE.setValue(key, new JSONObject().put("status", value).toString());
	}

}
