package com.ssafy.withy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class WithyApplication {

	public static void main(String[] args) {
		SpringApplication.run(WithyApplication.class, args);
	}

}
