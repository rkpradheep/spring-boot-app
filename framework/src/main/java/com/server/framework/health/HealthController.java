package com.server.framework.health;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.framework.common.AppStarted;

@RestController
@RequestMapping("/_app")
public class HealthController
{

	@GetMapping("/health")
	public ResponseEntity<String> health()
	{
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_PLAIN).body(AppStarted.APP_STARTED.toString());
	}

}
