package com.genai.advrag.isupport;

import com.genai.advrag.isupport.config.IngestionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IngestionProperties.class)
public class IsupportApplication {

	public static void main(String[] args) {

        SpringApplication.run(IsupportApplication.class, args);
	}

}
