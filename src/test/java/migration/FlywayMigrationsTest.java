package migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.agroal.api.AgroalDataSource;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class FlywayMigrationsTest {

    @Inject
    AgroalDataSource dataSource;

    @Test
    void shouldCreateAllCoreTables() throws SQLException {
        assertTrue(tableExists("tasks"));
        assertTrue(tableExists("tags"));
        assertTrue(tableExists("task_tags"));
        assertTrue(tableExists("task_audit"));
    }

    @Test
    void shouldCreateExpectedColumnsAndIndexes() throws SQLException {
        assertTrue(columnExists("tasks", "deleted_at"));
        assertTrue(columnExists("tasks", "version"));
        assertTrue(columnExists("task_audit", "before_snapshot"));
        assertTrue(columnExists("task_audit", "after_snapshot"));

        assertTrue(indexExists("idx_tasks_deleted_at"));
        assertTrue(indexExists("idx_tasks_status_deleted_at"));
        assertTrue(indexExists("idx_tasks_due_date_deleted_at"));
        assertTrue(indexExists("idx_task_tags_tag_id"));
    }

    private boolean tableExists(String tableName) throws SQLException {
        return count(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_SCHEMA) = 'PUBLIC' AND UPPER(TABLE_NAME) = UPPER(?)",
                tableName) == 1;
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        return count(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_SCHEMA) = 'PUBLIC' AND UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)",
                tableName,
                columnName) == 1;
    }

    private boolean indexExists(String indexName) throws SQLException {
        return count(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(TABLE_SCHEMA) = 'PUBLIC' AND UPPER(INDEX_NAME) = UPPER(?)",
                indexName) >= 1;
    }

    private int count(String sql, String... args) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setString(i + 1, args[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
