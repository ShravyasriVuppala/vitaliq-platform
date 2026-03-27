package com.vitaliq.vitaliq_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class VitalIqApplication {

	public static void main(String[] args) {
		SpringApplication.run(VitalIqApplication.class, args);
	}

}
