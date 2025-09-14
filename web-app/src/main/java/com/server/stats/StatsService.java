package com.server.stats;

import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.server.framework.error.AppException;
import com.server.framework.http.HttpService;
import com.server.framework.security.SecurityUtil;
import com.server.stats.meta.PlaceHolderMeta;
import com.server.stats.meta.RequestMeta;
import com.server.stats.meta.StatsMeta;

public class StatsService
{
	private static final Logger LOGGER = Logger.getLogger(StatsService.class.getName());

	static List<Map<String, String>> getRequestRowList(StatsMeta statsMeta) throws Exception
	{
		CSVParser inputDataParser;

		if(StringUtils.isNotEmpty(statsMeta.getRequestFilePath()))
		{
			inputDataParser = new CSVParser(new FileReader(statsMeta.getRequestFilePath()), CSVFormat.DEFAULT);
		}
		else
		{
			inputDataParser = new CSVParser(statsMeta.getRequestDataReader(), CSVFormat.DEFAULT);
		}
		int requestDataCount = 0;
		for(CSVRecord csvRecord : inputDataParser.getRecords())
		{
			requestDataCount++;
			if(requestDataCount == 1 && statsMeta.isSkipFirstRow())
			{
				continue;
			}
			Map<String, String> requestMap = new HashMap<>();
			for(int i = 0; i < csvRecord.size(); i++)
			{
				requestMap.put("${Col_" + i + "}", csvRecord.get(i));
			}
			statsMeta.getRequestMeta().addRequest(requestMap);
			if(statsMeta.isTest() && requestDataCount == 2)
			{
				break;
			}
		}
		return statsMeta.getRequestMeta().getRequestRowList();
	}

	static ImmutableTriple<String, Map<String, String>, JSONObject> handlePlaceholder(StatsMeta statsMeta, int requestCount, Map<String, String> requestDataRow)
	{
		String connectionUrl = statsMeta.getRequestMeta().getConnectionUrl();
		Map<String, String> params = new HashMap<>();
		JSONObject jsonObject = statsMeta.getRequestMeta().getJsonPayload();

		long currentTime = System.nanoTime();
		Map<String, String> phMeta = new HashMap<>();
		phMeta.put("${RequestNo}", requestCount + "");
		phMeta.put("${CurrentTime}", currentTime + "");

		phMeta.putAll(requestDataRow);

		connectionUrl = replacePH(connectionUrl, phMeta);
		statsMeta.getRequestMeta().getParamsMap().forEach((key, value) ->
		{
			params.put(key, replacePH(value, phMeta));
			modifyJSONPayload(jsonObject, key, value, phMeta);
		});

		phMeta.forEach((key, value) -> modifyJSONPayload(jsonObject, key, value, phMeta));

		return new ImmutableTriple<>(connectionUrl, params, jsonObject);
	}

	static String replacePH(String value, Map<String, String> phMeta)
	{
		for(Map.Entry<String, String> phMetaEntrySet : phMeta.entrySet())
		{
			value = value.replace(phMetaEntrySet.getKey(), phMetaEntrySet.getValue());
		}
		return value;
	}

	static void modifyJSONPayload(JSONObject jsonObject, String phKey, String phValue, Map<String, String> phMeta)
	{
		if(Objects.isNull(jsonObject))
		{
			return;
		}
		for(String key : jsonObject.keySet())
		{
			if(jsonObject.get(key) instanceof JSONObject)
			{
				modifyJSONPayload(jsonObject.getJSONObject(key), phKey, phValue, phMeta);
			}
			else if(jsonObject.get(key) instanceof JSONArray)
			{
				modifyJSONArray(jsonObject.getJSONArray(key), phKey, phValue, phMeta);
			}
			else
			{
				Object value = jsonObject.get(key);
				String modifiedValue = replacePH(value.toString(), phMeta);
				jsonObject.put(key, stringToObject(modifiedValue, value.getClass()));
			}
		}
	}

	static Object stringToObject(String value, Class<?> type)
	{
		if(type == Integer.class)
		{
			return Integer.valueOf(value);
		}
		else if(type == Double.class)
		{
			return Double.valueOf(value);
		}
		else if(type == Float.class)
		{
			return Float.valueOf(value);
		}
		else if(type == Boolean.class)
		{
			return Boolean.valueOf(value);
		}
		else if(type == BigDecimal.class)
		{
			return new BigDecimal(value);
		}
		else
		{
			return value;
		}
	}

	static void modifyJSONArray(JSONArray jsonArray, String phKey, String phValue, Map<String, String> phMeta)
	{
		for(int i = 0; i < jsonArray.length(); i++)
		{
			if(jsonArray.get(i) instanceof JSONObject)
			{
				modifyJSONPayload(jsonArray.getJSONObject(i), phKey, phValue, phMeta);
			}
			else if(jsonArray.get(i) instanceof JSONArray)
			{
				modifyJSONArray(jsonArray, phKey, phValue, phMeta);
			}
			else
			{
				jsonArray.put(i, replacePH(jsonArray.getString(i), phMeta));
			}
		}
	}

	static String getColumnValue(StatsMeta statsMeta, String responseColumnName, Triple<String, Map<String, String>, JSONObject> placeHolderTriple, String response, int requestCount, Map<String, String> requestDataRow)
	{
		try
		{
			JSONObject responseJSON = new JSONObject(SecurityUtil.isValidJSON(response) ? response : "{}");
			String responseColumnValue = statsMeta.getResponseColumnValue(responseColumnName);

			Object columnValue;

			Pattern pattern = Pattern.compile("\\$\\{params\\[(\\w+)\\]\\}");
			Matcher paramPHMatcher = pattern.matcher(responseColumnValue);

			Pattern jsonPattern = Pattern.compile("\\$\\{json_body\\[(\\w+)\\]\\}");
			Matcher jsonPHMatcher = jsonPattern.matcher(responseColumnValue);

			Pattern columnPattern = Pattern.compile("\\$\\{Col_\\d+\\}");
			Matcher columnPHMatcher = columnPattern.matcher(responseColumnValue);

			if(paramPHMatcher.matches() && placeHolderTriple.getMiddle() != null)
			{
				columnValue = placeHolderTriple.getMiddle().get(paramPHMatcher.group(1));
			}
			else if(columnPHMatcher.matches())
			{
				columnValue = requestDataRow.get(responseColumnValue);
			}
			else if(jsonPHMatcher.matches())
			{
				columnValue = getJSONPHValue(placeHolderTriple.getRight(), jsonPHMatcher.group(1));
			}
			else
			{
				if(StringUtils.equals(responseColumnValue, "${Response}"))
				{
					columnValue = response;
				}
				else if(StringUtils.contains(responseColumnValue, "${RequestNo}"))
				{
					columnValue = requestCount;
				}
				else if(StringUtils.contains(responseColumnValue, "${Response}."))
				{
					columnValue = getJSONPHValue(responseJSON, responseColumnValue.replace("${Response}.", ""));
				}
				else
				{
					columnValue = "Error";
				}
			}
			PlaceHolderMeta placeHolderMeta = new PlaceHolderMeta(responseColumnName, columnValue);
			placeHolderMeta.setStatsMeta(statsMeta);
			placeHolderMeta.setRequestDataRow(requestDataRow);
			return statsMeta.isPHResponseColumn(responseColumnName) ? statsMeta.getPlaceholderHandlerFunction().apply(placeHolderMeta) : String.valueOf(columnValue);
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred", e);
			return null;
		}
	}

	static Object getJSONPHValue(JSONObject jsonObject, String jsonPH)
	{
		JSONObject temp = new JSONObject(jsonObject.toString());

		String[] columnValuePHArray = jsonPH.split("/");

		for(String columnValuePH : columnValuePHArray)
		{
			Matcher jsonArrayPatternMatcher = Pattern.compile("(\\w+)\\[(\\d+)\\]").matcher(columnValuePH);
			if(columnValuePHArray[columnValuePHArray.length - 1].equals(columnValuePH))
			{
				return temp.opt(columnValuePH);
			}
			if(!temp.has(columnValuePH))
			{
				return null;
			}
			temp = !jsonArrayPatternMatcher.matches() ? new JSONObject(temp.get(columnValuePH).toString()) : (JSONObject) new JSONArray(temp.get(jsonArrayPatternMatcher.group(1)).toString()).get(Integer.parseInt(jsonArrayPatternMatcher.group(2)));
		}
		return null;
	}

	static Node getNode(Element element, String name)
	{
		//return element.getElementsByTagName("rkp:" + name).item(0);
		return element.getElementsByTagName(name).item(0);
	}

	static String getTextContent(Node node)
	{
		//return element.getElementsByTagName("rkp:" + name).item(0);
		return Objects.isNull(node) ? null : node.getTextContent();
	}

	static Element getElement(Document document, String name)
	{
		//return (Element) document.getElementsByTagName("rkp:" + name).item(0);
		return (Element) document.getElementsByTagName(name).item(0);
	}

	static RequestMeta getRequestMeta(String connectionUrl, Element paramsElement, String jsonBody) throws Exception
	{
		RequestMeta requestMeta = new RequestMeta();
		requestMeta.setConnectionUrl(connectionUrl);
		requestMeta.setJsonPayload(StringUtils.defaultIfEmpty(jsonBody, StringUtils.EMPTY).trim());

		if(Objects.isNull(paramsElement))
		{
			return requestMeta;
		}

		NodeList paramNodeList = paramsElement.getChildNodes();

		for(int i = 0; i < paramNodeList.getLength(); i++)
		{
			Node paramNode = paramNodeList.item(i);
			if(paramNode.getNodeType() == Node.ELEMENT_NODE)
			{
				requestMeta.addParam(paramNode.getNodeName(), paramNode.getTextContent());
			}
		}

		return requestMeta;
	}

	static StatsMeta getStatsMeta(InputStream configurationFile, Reader requestFileReader, String customResponseFilePath) throws Exception
	{
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source schemaFile = new StreamSource(StatsService.class.getClassLoader().getResourceAsStream("conf/stats-meta.xsd"));
		Schema schema = factory.newSchema(schemaFile);
		Validator validator = schema.newValidator();
		validator.validate(new StreamSource(configurationFile));

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		configurationFile.reset();
		Document doc = db.parse(configurationFile);
		doc.getDocumentElement().normalize();

		Element configuration = getElement(doc, "configuration");

		String connectionUrl = getNode(configuration, "url").getTextContent();
		String method = getNode(configuration, "method").getTextContent();
		int requestBatchSize = Integer.parseInt(getNode(configuration, "request-batch-size").getTextContent());
		int requestIntervalSeconds = Integer.parseInt(getNode(configuration, "request-batch-interval").getTextContent());
		String jsonPayload = getTextContent(getNode(configuration, "json-body"));
		String requestFilePath = getTextContent(getNode(configuration, "input-file-path"));
		Node responseFilePathNode = getNode(configuration, "output-file-path");
		String responseFilePath = Objects.isNull(responseFilePathNode) ? customResponseFilePath : responseFilePathNode.getTextContent();
		Node processResponseHandlerMethodNode = getNode(configuration, "placeholder-handler");
		String processResponseHandlerMethod = getTextContent(processResponseHandlerMethodNode);
		processResponseHandlerMethod = StringUtils.defaultIfEmpty(processResponseHandlerMethod, StringUtils.EMPTY).trim();
		Function<PlaceHolderMeta, String> placeholderHandlerFunction = StringUtils.isEmpty(processResponseHandlerMethod) ? null : (Function<PlaceHolderMeta, String>) Class.forName("com.server.stats.StatsAPIPlaceholderHandler").getDeclaredMethod(processResponseHandlerMethod).invoke(null);
		Node isTestNode = getNode(configuration, "is-test");
		boolean isTest = Objects.nonNull(isTestNode) && Boolean.parseBoolean(isTestNode.getTextContent());
		Node skipFirstRowNode = getNode(configuration, "skip-first-request-data-row");
		boolean skipFirstRow = Objects.nonNull(skipFirstRowNode) && Boolean.parseBoolean(skipFirstRowNode.getTextContent());

		Node disableParallelNode = getNode(configuration, "disable-parallel-calls");
		boolean disableParallelCalls = Objects.nonNull(isTestNode) && Boolean.parseBoolean(disableParallelNode.getTextContent());

		StatsMeta statsMeta = new StatsMeta();
		statsMeta.setMethod(method);
		statsMeta.setRequestMeta(getRequestMeta(connectionUrl, (Element) getNode(configuration, "params"), jsonPayload));
		statsMeta.setRequestBatchSize(requestBatchSize);
		statsMeta.setRequestIntervalSeconds(requestIntervalSeconds);
		statsMeta.setPlaceholderHandlerFunction(placeholderHandlerFunction);
		statsMeta.setRequestFilePath(requestFilePath);
		String[] filePathExtensionArray = responseFilePath.split("\\.");
		statsMeta.setResponseFilePath(filePathExtensionArray.length == 0 ? responseFilePath + "_inprocess" : filePathExtensionArray[0] + "_inprocess." + filePathExtensionArray[1]);
		statsMeta.setRequestDataReader(requestFileReader);
		statsMeta.setTest(isTest);
		statsMeta.setSkipFirstRow(skipFirstRow);
		statsMeta.setDisableParallelCalls(disableParallelCalls);

		Element headersElement = (Element) getNode(configuration, "headers");
		if(Objects.nonNull(headersElement))
		{
			NodeList headerNodeList = headersElement.getChildNodes();

			for(int i = 0; i < headerNodeList.getLength(); i++)
			{
				if(headerNodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
				{
					statsMeta.addRequestHeader(headerNodeList.item(i).getNodeName(), headerNodeList.item(i).getTextContent());
				}
			}
		}

		headersElement = (Element) getNode(configuration, "raw-request-headers");
		if(Objects.nonNull(headersElement))
		{
			String rawHeaders = headersElement.getTextContent();
			HttpService.convertRawHeadersToMapStatic(rawHeaders).entrySet().forEach(entry -> statsMeta.addRequestHeader(entry.getKey(), entry.getValue()));
		}

		Element responseElement = (Element) getNode(configuration, "response");
		NodeList responseChildNodeList = responseElement.getChildNodes();

		StringBuilder responseHeaders = new StringBuilder();

		Set<String> headersSet = new HashSet<>();

		for(int i = 0; i < responseChildNodeList.getLength(); i++)
		{
			if(responseChildNodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
			{
				String headerName = responseChildNodeList.item(i).getAttributes().getNamedItem("name").getTextContent();
				if(!headersSet.add(headerName))
				{
					continue;
				}
				Node phNode = responseChildNodeList.item(i).getAttributes().getNamedItem("placeholder");
				boolean isPlaceholder = phNode != null && Boolean.valueOf(phNode.getTextContent());
				if(isPlaceholder && Objects.isNull(statsMeta.getPlaceholderHandlerFunction()))
				{
					throw new AppException("Value For Placeholder handler is not passed but configured for header: " + headerName);
				}
				responseHeaders.append(headerName).append(",");
				statsMeta.addResponseColumn(headerName, responseChildNodeList.item(i).getTextContent(), isPlaceholder);
			}
		}

		statsMeta.setResponseHeaders(responseHeaders.toString().replaceAll(",$", ""));
		return statsMeta;
	}
}
