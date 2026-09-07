package com.drukconnect.drukconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DrukconnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(DrukconnectApplication.class, args);
	}

}
