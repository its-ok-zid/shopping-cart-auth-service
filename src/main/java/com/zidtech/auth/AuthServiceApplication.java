package com.zidtech.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Tell Spring to scan both the local auth package and the imported security library package
@SpringBootApplication(scanBasePackages = {
		"com.zidtech.auth",
		"com.zidtech.security"
})
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}