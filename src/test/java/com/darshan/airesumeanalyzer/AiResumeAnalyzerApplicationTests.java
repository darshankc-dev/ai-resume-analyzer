package com.darshan.airesumeanalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Spring application context starts without errors.
 * Uses a random port and disables JPA auto-config for test isolation
 * (no PostgreSQL required to run tests).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    }
)
class AiResumeAnalyzerApplicationTests {

    @Test
    void contextLoads() {
        // Context loads successfully without a database connection.
    }
}
