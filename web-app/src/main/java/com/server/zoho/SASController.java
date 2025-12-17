package com.server.zoho;

import com.server.framework.common.AppProperties;
import com.server.framework.common.CommonService;
import com.server.framework.error.AppException;
import com.server.framework.service.DatabaseService;
import com.server.framework.builder.ApiResponseBuilder;
import com.server.framework.factory.DatabaseConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

@RestController
@RequestMapping("/api/v1/sas")
public class SASController
{

	private static final Logger LOGGER = Logger.getLogger(SASController.class.getName());

	@Autowired
	private DatabaseService databaseService;
	
	@Autowired
	private DatabaseConnectionFactory connectionFactory;

	@Autowired
	private SASService sasService;

	@Autowired
	private StatsQueryTool statsQueryTool;

	private static SASController instance;

	@PostConstruct
	public void init()
	{
		instance = this;
	}

	public static Map<String, Object> handleSasRequestStatic(JSONObject credentials) throws Exception
	{
		if(instance == null)
		{
			throw new IllegalStateException("SASController instance not initialized");
		}
		return instance.handleSasRequest(credentials);
	}

	@GetMapping("/limits/{id}")
	public ResponseEntity<Map<String, Object>> getLimits(@PathVariable String id, @RequestParam(required = false) Boolean pk)
	{
		try
		{
			Map<String, Object> data;
			if(pk == null || !pk)
			{
				long[] limits = sasService.getLimits(Long.valueOf(id));
				Map<String, Long> responseMap = new LinkedHashMap<>();
				responseMap.put("lower_limit", limits[0]);
				responseMap.put("upper_limit", limits[1]);
				data = new HashMap<>(responseMap);
			}
			else
			{
				String spaceId = sasService.getSpaceIDFromPK(Long.valueOf(id));
				data = new HashMap<>();
				data.put("space_id", spaceId);
			}

			Map<String, Object> response = ApiResponseBuilder.success("Limits retrieved successfully", data);
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred", e);
			Map<String, Object> response = ApiResponseBuilder.error("Failed to retrieve limits: " + e.getMessage(), 400);
			return ResponseEntity.badRequest().body(response);
		}
	}

	@PostMapping("/execute")
	public ResponseEntity<Map<String, Object>> execute(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest, HttpServletResponse response) throws Exception
	{
		try
		{
			JSONObject credentials = new JSONObject(request);

			boolean isEncrypted = credentials.optBoolean("is_encrypted");
			boolean isStats = credentials.optBoolean("is_stats");

			if(isEncrypted)
			{
				sasService.handleDecryption(httpRequest, credentials);
			}

			if(isStats)
			{
				Map<String, String> statsResult = statsQueryTool.handleStats(credentials);
				Map<String, Object> apiResponse = ApiResponseBuilder.success("Stats executed successfully", statsResult);
				return ResponseEntity.ok(apiResponse);
			}

			boolean tableOrColumnRequest = credentials.optBoolean("need_table") || credentials.optBoolean("need_column");

			if(tableOrColumnRequest)
			{
				Map<String, Object> metaResult = handleTableOrColumnMeta(credentials);
				Map<String, Object> apiResponse = ApiResponseBuilder.success("Meta data retrieved successfully", metaResult);
				return ResponseEntity.ok(apiResponse);
			}

			Map<String, Object> sasResult = handleSasRequest(credentials);
			Map<String, Object> apiResponse = ApiResponseBuilder.success("SAS request executed successfully", sasResult);
			return ResponseEntity.ok(apiResponse);

		}
		catch(AppException ae)
		{
			LOGGER.log(Level.SEVERE, "AppException occurred", ae);
			throw ae;
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	@GetMapping("/meta")
	public ResponseEntity<Map<String, Object>> getMeta(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			Map<String, Object> metaData = getSasMeta(request, response);
			Map<String, Object> apiResponse = ApiResponseBuilder.success("SAS meta retrieved successfully", metaData);
			return ResponseEntity.ok(apiResponse);
		}
		catch(Exception e)
		{
			LOGGER.severe("Error in getMeta: " + e.getMessage());
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in getMeta", e);
			Map<String, Object> errorResponse = ApiResponseBuilder.error("Service not available: " + e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	public Map<String, Object> handleSasRequest(JSONObject credentials) throws Exception
	{
		String server = credentials.getString("server");
		String ip = credentials.getString("ip");
		String user = credentials.getString("user");
		String password = credentials.getString("password");
		String zsid = credentials.optString("zsid");
		String pk = credentials.optString("pk");
		String query = credentials.optString("query", "");
		boolean skipScoping = credentials.optBoolean("skip_scoping");

		try(Connection conn = connectionFactory.createConnection(server, ip, "jbossdb", user, password))
		{
			zsid = !zsid.equals("") && StringUtils.isEmpty(pk) ? zsid : sasService.getZSIDFromPK(conn, pk);
			PreparedStatement statement = conn.prepareStatement(AppProperties.getProperty("sas.space.details.query"));
			statement.setString(1, zsid);
			statement.execute();
			ResultSet resultSet = statement.getResultSet();
			boolean exist = resultSet.next();
			if(!exist)
			{
				throw new AppException("Invalid " + (StringUtils.isEmpty(pk) ? "zsid" : "PK value " + " provided."));
			}
			long[] limits = sasService.getLimits(Long.valueOf(resultSet.getString("ID")));
			String schema = resultSet.getString("SCHEMANAME");

			Map<String, Object> responseMap = new LinkedHashMap<>();

			responseMap.put("schema_name", schema);
			responseMap.put("zsid", zsid);

			String clusterIP = sasService.getClusterIP(conn, resultSet.getString("DBMASTERID"));
			responseMap.put("cluster_ip", clusterIP);

			Map<String, Object> limitsDetails = new LinkedHashMap<>();

			limitsDetails.put("sas_range_begin", limits[0]);
			limitsDetails.put("sas_range_end", limits[1]);

			responseMap.put("sas_range", limitsDetails);

			Map<String, Object> finalResponse = new LinkedHashMap<>();
			finalResponse.put("sas_meta", responseMap);
			sasService.handleQuery(query, server, clusterIP, schema, user, password, finalResponse, limits[0], limits[1], skipScoping);

			return finalResponse;
		}
	}


	public Map<String, Object> getServicesCredentials(PublicKey publicKey)
	{
		Map<String, Object> map = new HashMap<>();
		for(String service : AppProperties.getProperty("db.services").split(","))
		{
			Map<String, String> serviceCredentials = new HashMap<>();

			String ip = AppProperties.getProperty("db.$.ip".replace("$", service));
			String user = AppProperties.getProperty("db.$.user".replace("$", service));
			String password = AppProperties.getProperty("db.$.password".replace("$", service));
			String server = AppProperties.getProperty("db.$.server".replace("$", service));

			serviceCredentials.put("ip", Objects.nonNull(publicKey) ? CommonService.encryptData(publicKey, ip) : ip);
			serviceCredentials.put("user", Objects.nonNull(publicKey) ? CommonService.encryptData(publicKey, user) : user);
			serviceCredentials.put("password", Objects.nonNull(publicKey) ? CommonService.encryptData(publicKey, password) : password);
			serviceCredentials.put("server", Objects.nonNull(publicKey) ? CommonService.encryptData(publicKey, server) : server);

			map.put(service, serviceCredentials);
		}

		return map;
	}

	private Map<String, Object> handleTableOrColumnMeta(JSONObject credentials) throws Exception
	{
		String server = credentials.getString("server");
		String ip = credentials.getString("ip");
		String user = credentials.getString("user");
		String password = credentials.getString("password");

		try(Connection connection = connectionFactory.createConnection(server, ip, "jbossdb", user, password))
		{
			if(credentials.optBoolean("need_table"))
			{
				DatabaseMetaData databaseMetaData = connection.getMetaData();
				Set<String> tableSet = new HashSet<>();
				ResultSet tableResultSet = databaseMetaData.getTables(null, "jbossdb", "%", new String[] {"TABLE"});
				while(tableResultSet.next())
				{
					try
					{
						if(!tableResultSet.getString("TABLE_NAME").equalsIgnoreCase("Table"))
							tableSet.add(tableResultSet.getString("TABLE_NAME"));
					}
					catch(Exception e)
					{
					}
				}
				Map<String, Object> result = new HashMap<>();
				result.put("tables", tableSet);
				result.put("is_multigrid", sasService.isMultiGrid(connection));
				return result;
			}

			String tableName = credentials.optString("table");
			if(tableName == null || tableName.trim().isEmpty())
			{
				throw new IllegalArgumentException("Table name is required when need_column is true");
			}
			return handleColumnMeta(connection, tableName);
		}
	}

	private Map<String, Object> handleColumnMeta(Connection connection, String table) throws Exception
	{
		DatabaseMetaData databaseMetaData = connection.getMetaData();

		Map<String, Object> result = new LinkedHashMap<>();
		List<String> columnList = new ArrayList<>();
		List<String> bigIntColumns = new ArrayList<>();
		String pkName = "";
		ResultSet columnResultSet = databaseMetaData.getColumns(null, "jbossdb", table, null);
		while(columnResultSet.next())
		{
			String columnName = columnResultSet.getString("COLUMN_NAME").toUpperCase();
			columnList.add(columnName);
			if(columnResultSet.getInt("DATA_TYPE") == Types.BIGINT)
			{
				bigIntColumns.add(columnName);
			}
		}
		if(!bigIntColumns.isEmpty())
		{
			ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(null, "jbossdb", table);

			while(primaryKeys.next())
			{
				String pk = primaryKeys.getString("COLUMN_NAME").toUpperCase();
				if(bigIntColumns.contains(pk))
				{
					pkName = pk;
					break;
				}
			}
		}
		result.put("columns", columnList);
		result.put("pk", pkName);

		return result;
	}

	private Map<String, Object> getSasMeta(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		PrivateKey privateKey = (PrivateKey) request.getSession().getAttribute("private_key");
		PublicKey publicKey = (PublicKey) request.getSession().getAttribute("public_key");

		if(privateKey == null)
		{
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
			SecureRandom random = SecureRandom.getInstanceStrong();
			keyPairGen.initialize(2048, random);
			KeyPair keyPair = keyPairGen.generateKeyPair();

			privateKey = keyPair.getPrivate();
			publicKey = keyPair.getPublic();

			request.getSession().setAttribute("private_key", privateKey);
			request.getSession().setAttribute("public_key", publicKey);

			StringBuilder header = new StringBuilder();
			header.append("JSESSIONID=").append(request.getSession().getId());
			header.append("; Secure");
			header.append("; SameSite=None");
			response.setHeader("Set-Cookie", header.toString());
		}

		Map<String, Object> map = getServicesCredentials(publicKey);

		Map<String, String> iscMeta = new HashMap<>();

		for(String service : AppProperties.getProperty("security.services").split(","))
		{
			iscMeta.put(service, AppProperties.getProperty("zoho." + service + ".display.name"));
		}

		map.put("isc_meta", iscMeta);

		Map<String, String> earMeta = new HashMap<>();

		for(String service : AppProperties.getProperty("ear.services").split(","))
		{
			earMeta.put(service, AppProperties.getProperty("zoho." + service + ".display.name"));
		}

		map.put("ear_meta", earMeta);

		Map<String, Object> dbMeta = new LinkedHashMap<>();

		for(String service : AppProperties.getProperty("db.services").split(","))
		{
			dbMeta.put(service, Map.of("server", AppProperties.getProperty("db.".concat(service).concat(".").concat("server")), "display_name", AppProperties.getProperty("zoho." + service + ".display.name")));
		}

		map.put("db_meta", dbMeta);

		Map<String, Object> taskEngineMeta = new HashMap<>();

		Map<String, Object> products = new LinkedHashMap<>();
		for(String service : AppProperties.getProperty("taskengine.services").split(","))
		{
			products.put(service, AppProperties.getProperty("zoho." + service + ".display.name"));
		}

		taskEngineMeta.put("products", products);

		Map<String, List<String>> threadPoolMeta = new HashMap<>();

		for(String service : AppProperties.getProperty("taskengine.services").split(","))
		{
			threadPoolMeta.put(service.split("-")[0], Arrays.stream(AppProperties.getProperty("taskengine." + service.split("-")[0] + ".threadpools").split(",")).collect(Collectors.toList()));
		}

		taskEngineMeta.put("thread_pools", threadPoolMeta);

		map.put("taskengine_meta", taskEngineMeta);

		return map;
	}
}
