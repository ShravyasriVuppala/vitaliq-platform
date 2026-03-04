package com.vitaliq.vitaliq_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class VitalIqApplication {

	public static void main(String[] args) {
		SpringApplication.run(VitalIqApplication.class, args);
	}

}
