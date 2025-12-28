package com.nemal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SchedulerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchedulerBackendApplication.class, args);
	}

}
