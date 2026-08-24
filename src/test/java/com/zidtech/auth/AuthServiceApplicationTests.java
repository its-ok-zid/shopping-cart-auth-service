package com.zidtech.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

// We inject a dummy valid Base64 secret strictly for the test environment
@SpringBootTest(properties = {
		"app.security.jwt-secret=dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1zaG9wcGluZy1jYXJ0"
})
@AutoConfigureTestDatabase
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
		// Just verifies the Spring Context can successfully boot up using H2
	}

}