package com.adarsh.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

	private Logger logger = LogManager.getLogger(Controller.class);
	@GetMapping("/api/hello")
	ResponseEntity<String> getHello() {
		logger.info("Hello world from log4j");
		return ResponseEntity.ok("Hello World!!");
	}
}
