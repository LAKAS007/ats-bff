package com.diploma.atsbff;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@ConfigurationPropertiesScan
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AtsBffApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtsBffApplication.class, args);
	}

}
