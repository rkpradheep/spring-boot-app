package com.server.zoho;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.server.framework.common.AppContextHolder;
import com.server.framework.common.AppProperties;
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

	public static void downloadBuilds(String oldBuildUrl, String newBuildUrl, InputStream newBuildFileInputStream, Path tempDir, String referenceID) throws Exception
	{
		try
		{
			ConfigurationEntity configurationEntity = CONFIGURATION_SERVICE.get(Long.parseLong(referenceID)).get();

			CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "DOWNLOADING_OLD_BUILD");

			HttpContext httpContext = new HttpContext(oldBuildUrl, "GET");
			httpContext.setHeader("Authorization", "Basic " + AppProperties.getProperty("zoho.build.download.basic.header.token"));
			HttpResponse httpResponse = HttpService.makeNetworkCallStatic(httpContext);

			InputStream inputStream = httpResponse.getInputStream();

			Path oldBuildPath = tempDir.resolve("old_build.zip");
			Files.copy(inputStream, oldBuildPath);
			inputStream.close();

			if(Objects.nonNull(newBuildFileInputStream))
			{
				Path newBuildPath = tempDir.resolve("new_build.zip");
				Files.copy(newBuildFileInputStream, newBuildPath);
				newBuildFileInputStream.close();
			}
			else
			{
				CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "DOWNLOADING_NEW_BUILD");

				httpContext = new HttpContext(newBuildUrl, "GET");
				httpContext.setHeader("Authorization", "Basic " + AppProperties.getProperty("zoho.build.download.basic.header.token"));
				httpResponse = HttpService.makeNetworkCallStatic(httpContext);

				inputStream = httpResponse.getInputStream();
				Path newBuildPath = tempDir.resolve("new_build.zip");
				Files.copy(inputStream, newBuildPath);
				inputStream.close();
			}
			generateDiffReport(referenceID);

			CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "COMPLETED");
		}
		catch(Exception e)
		{
			ConfigurationEntity configurationEntity = CONFIGURATION_SERVICE.get(Long.parseLong(referenceID)).get();
			CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), configurationEntity.getCValue().concat("_FAILED"));
			throw e;
		}
	}

	public static void generateDiffReport(String referenceID) throws Exception
	{
		ConfigurationEntity configurationEntity = CONFIGURATION_SERVICE.get(Long.parseLong(referenceID)).get();

		Path tempDir = Path.of(configurationEntity.getCKey());

		CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "UNZIPPING_OLD_BUILD");
		unzip(tempDir.resolve("old_build.zip").toString(), tempDir.resolve("old_build").toString());

		CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "UNZIPPING_NEW_BUILD");
		unzip(tempDir.resolve("new_build.zip").toString(), tempDir.resolve("new_build").toString());

		CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "EXTRACTING_OLD_BUILD_ROOT_WAR");
		unzip(tempDir.resolve("old_build/AdventNet/Sas/tomcat/webapps/ROOT.war").toString(), tempDir.resolve("old_build/AdventNet/Sas/tomcat/webapps/ROOT").toString());

		CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "EXTRACTING_NEW_BUILD_ROOT_WAR");
		unzip(tempDir.resolve("new_build/AdventNet/Sas/tomcat/webapps/ROOT.war").toString(), tempDir.resolve("new_build/AdventNet/Sas/tomcat/webapps/ROOT").toString());


		String content = Files.readString(tempDir.resolve("old_build/AdventNet/Sas/bin/sqlCreation.sh"), StandardCharsets.UTF_8);
		content = content.replace("generate.destructive.changes=false", "generate.destructive.changes=true");

		Files.writeString(tempDir.resolve("old_build/AdventNet/Sas/bin/sqlCreation.sh"), content, StandardCharsets.UTF_8);

		CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "CHANGING_SQLCREATION_FILE_PERMISSION");

		String response = ShellCommandExecutionController.execute(new String[]{"bash", "-c",
			"chmod +x " + tempDir.resolve("old_build/AdventNet/Sas/bin/sqlCreation.sh")});

		LOGGER.log(Level.INFO, "SQL Creation Permission Change Response: {0}", response);

		CONFIGURATION_SERVICE.setValue(configurationEntity.getCKey(), "GENERATING_DIFF_REPORT");

		response = ShellCommandExecutionController.execute(new String[]{"bash", "-c",
			"sh sqlCreation.sh " + tempDir.resolve("old_build/AdventNet/Sas/tomcat/webapps/ROOT/WEB-INF") + "/" + " " + tempDir.resolve("new_build/AdventNet/Sas/tomcat/webapps/ROOT/WEB-INF") + "/" + " -Dgenerate.destructive.changes=true"
		}, tempDir.resolve("old_build/AdventNet/Sas/bin").toString());

		LOGGER.log(Level.INFO, "DD diff generation response : {0}", response);

	}

	public static void unzip(String zipFilePath, String destDir) throws IOException
	{
		File dir = new File(destDir);
		if(!dir.exists())
			dir.mkdirs();

		byte[] buffer = new byte[4096];

		try(ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath)))
		{
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null)
			{
				File newFile = new File(destDir, entry.getName());
				if(entry.isDirectory())
				{
					newFile.mkdirs();
				}
				else
				{
					newFile.getParentFile().mkdirs();

					try(FileOutputStream fos = new FileOutputStream(newFile))
					{
						int len;
						while((len = zis.read(buffer)) > 0)
						{
							fos.write(buffer, 0, len);
						}
					}
				}

				zis.closeEntry();
			}
		}
	}
}
