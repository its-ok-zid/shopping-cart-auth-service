package com.zidtech.auth;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthServiceApplicationTests {

	@Test
	void testMainMethodAndConstructor() {
		// 1. Test the constructor to get 100% method coverage
		AuthServiceApplication app = new AuthServiceApplication();
		assertNotNull(app);

		// 2. Test the main method safely by intercepting SpringApplication.run
		try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
			AuthServiceApplication.main(new String[]{});
			mocked.verify(() -> SpringApplication.run(AuthServiceApplication.class, new String[]{}));
		}
	}
}