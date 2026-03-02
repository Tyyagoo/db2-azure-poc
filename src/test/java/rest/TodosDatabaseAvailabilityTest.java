package rest;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodosDatabaseAvailabilityTest {

    @Test
    void shouldDetectConnectionStateInSqlExceptionChain() {
        SQLException sqlException = new SQLException("DB2 down", "08001");
        RuntimeException wrapped = new RuntimeException("wrapper", sqlException);

        assertTrue(Todos.isDatabaseUnavailable(wrapped));
    }

    @Test
    void shouldIgnoreNonConnectionSqlState() {
        SQLException sqlException = new SQLException("syntax error", "42601");

        assertFalse(Todos.isDatabaseUnavailable(sqlException));
    }
}
