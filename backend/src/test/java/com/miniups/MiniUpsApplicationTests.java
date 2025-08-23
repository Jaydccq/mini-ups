/**
 * Mini UPS Application Tests
 * 
 * Basic application context loading test for CI/CD validation.
 * Tests that the Spring Boot application can start successfully with test profile.
 */
package com.miniups;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class MiniUpsApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // in the test environment. A failure here indicates configuration issues.
    }
}