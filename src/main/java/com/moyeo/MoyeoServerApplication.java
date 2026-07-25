package com.moyeo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoyeoServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoyeoServerApplication.class, args);
	}

}
