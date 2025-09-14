package com.server.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.server.framework.builder.ApiResponseBuilder;
import com.server.framework.common.CommonService;
import com.server.framework.http.FormData;
import com.server.framework.security.SecurityUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.AccessException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController
{
	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	@Autowired
	private AdminDBService adminDBService;

	@PostMapping("/db/execute")
	public ResponseEntity<Map<String, Object>> handleAdminDBRequest(@RequestBody String requestBody)
	{
		try
		{
			JSONObject credentials = new JSONObject(requestBody);
			String query = credentials.optString("query", "");
			boolean needTable = credentials.optBoolean("need_table", false);
			boolean needColumn = credentials.optBoolean("need_column", false);
			String table = credentials.optString("table", "");

			Object result = adminDBService.executeQuery(query, needTable, needColumn, table);
			Map<String, Object> response = ApiResponseBuilder.success("Query executed successfully", result);
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			logger.error("Error executing query", e);
			Map<String, Object> response = ApiResponseBuilder.error("Error executing query: " + e.getMessage(), "QUERY_EXECUTION_ERROR", 500);
			return ResponseEntity.internalServerError().body(response);
		}
	}

	@PostMapping("/file/transfer")
	public ResponseEntity<Map<String, Object>> handleFileTransfer(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		Map<String, FormData> formDataMap = SecurityUtil.parseMultiPartFormData(request);
		String path = formDataMap.get("path").getValue();
		path = path.replaceAll("/*$", StringUtils.EMPTY);
		File file = new File(CommonService.HOME_PATH, path);
		if(!file.getCanonicalPath().startsWith(CommonService.HOME_PATH))
		{
			throw new AccessException("Invalid path");
		}
		file.mkdirs();
		FormData.FileData fileData = formDataMap.get("file").getFileDataList().get(0);
		file = new File(CommonService.HOME_PATH, path + "/" + fileData.getFileName());
		try(FileOutputStream fileOutputStream = new FileOutputStream(file))
		{
			fileOutputStream.write(fileData.getBytes());
		}

		return ResponseEntity.ok(ApiResponseBuilder.create().message("File transferred successfully").build());
	}
}
