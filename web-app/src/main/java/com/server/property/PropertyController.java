package com.server.property;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import com.server.framework.common.AppProperties;

@RestController
@RequestMapping("/api/v1/admin/properties")
public class PropertyController
{

	@GetMapping
	public ResponseEntity<Object> getAllProperties(@RequestParam(value = "property_name", required = false) String propertyName)
	{

		if(StringUtils.isBlank(propertyName))
		{
			return ResponseEntity.ok(AppProperties.getPropertyList());
		}

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Property retrieved successfully");

		String sensitiveKeyRegex = "(.*)(password|credential|secret)(.*)";
		response.put("property_value", propertyName.matches(sensitiveKeyRegex) ? "*****" : AppProperties.getProperty(propertyName));

		return ResponseEntity.ok(response);
	}


	@PutMapping()
	public ResponseEntity<Map<String, Object>> updateProperty(@RequestBody Map<String, Object> request)
	{
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Property updated successfully");
		AppProperties.updateProperty((String) request.get("property_name"), (String) request.get("property_value"));
		return ResponseEntity.ok(response);
	}
}
