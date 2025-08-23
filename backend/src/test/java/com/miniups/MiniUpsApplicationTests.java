/**
 * Mini UPS Application Tests
 * 
 * Basic application context loading test for CI/CD validation.
 * Tests that the Spring Boot application can start successfully with test profile.
 */
package com.miniups;

import com.miniups.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, 
                properties = {
                    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
                })
@ActiveProfiles("test")
@Import(TestConfig.class)
class MiniUpsApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // in the test environment. A failure here indicates configuration issues.
    }
}