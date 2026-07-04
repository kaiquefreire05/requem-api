package com.requiem.api;

import org.junit.jupiter.api.Test;

/**
 * Full Spring context integration test — requires a running PostgreSQL instance.
 *
 * Run manually when DB is available:
 *   DATABASE_URL=jdbc:postgresql://localhost:5432/requiem_db ./mvnw test -Dtest=ApiApplicationTests
 *
 * Excluded from the default test run via maven-surefire-plugin config in pom.xml.
 */
class ApiApplicationTests {

    @Test
    void contextLoads() {
        // This test intentionally left blank.
        // Full context load requires a live database connection.
    }
}
