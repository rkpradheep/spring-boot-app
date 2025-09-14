package com.server.framework.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.server.framework.service.UserService;
import com.server.framework.dto.UserDto;
import com.server.framework.builder.ApiResponseBuilder;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserController
{

	@Autowired
	private UserService userService;

	@PostMapping
	public ResponseEntity<Map<String, Object>> createUser(@RequestBody UserDto userDto)
	{
		try
		{
			userService.createUser(userDto.getName(), userDto.getPassword(), userDto.getRoleType());
			Map<String, Object> response = ApiResponseBuilder.create().message("User created successfully").build();
			return ResponseEntity.ok(response);
		}
		catch(Exception e)
		{
			Map<String, Object> response = ApiResponseBuilder.error("Failed to create user: " + e.getMessage(), 500);
			return ResponseEntity.internalServerError().body(response);
		}
	}

}
