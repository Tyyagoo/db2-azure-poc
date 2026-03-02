package config;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class BaselineConfigurationTest {

    @Inject
    Config config;

    @Inject
    Flyway flyway;

    @Test
    void shouldUseH2DatasourceAndFlywayInTestProfile() {
        assertEquals("h2", config.getValue("quarkus.datasource.db-kind", String.class));

        String jdbcUrl = config.getValue("quarkus.datasource.jdbc.url", String.class);
        assertTrue(jdbcUrl.startsWith("jdbc:h2:mem:tasks"));
        assertTrue(jdbcUrl.contains("MODE=DB2"));

        assertEquals("true", config.getValue("quarkus.flyway.migrate-at-start", String.class));
        assertEquals("db/migration", config.getValue("quarkus.flyway.locations", String.class));
        assertNotNull(flyway);
    }
}
