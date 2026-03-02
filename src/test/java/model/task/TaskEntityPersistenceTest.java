package model.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TaskEntityPersistenceTest {

    @Inject
    EntityManager entityManager;

    @Inject
    AgroalDataSource dataSource;

    @Test
    @TestTransaction
    void shouldSetTimestampsAndVersionOnPersist() throws SQLException {
        TaskEntity task = new TaskEntity();
        task.setTitle("Write task entity tests");
        task.setStatus(TaskStatus.OPEN);

        entityManager.persist(task);
        entityManager.flush();

        assertNotNull(task.getId());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
        assertEquals(task.getCreatedAt(), task.getUpdatedAt());
        assertNotNull(task.getVersion());
        assertEquals(0L, task.getVersion());
        assertEquals("open", findStoredStatus(task.getId()));
    }

    @Test
    @TestTransaction
    void shouldAdvanceVersionAndUpdatedAtOnUpdate() {
        TaskEntity task = new TaskEntity();
        task.setTitle("Original title");
        task.setStatus(TaskStatus.OPEN);

        entityManager.persist(task);
        entityManager.flush();

        Long initialVersion = task.getVersion();
        Instant initialUpdatedAt = task.getUpdatedAt();

        task.setTitle("Updated title");
        entityManager.flush();

        assertTrue(task.getVersion() > initialVersion);
        assertTrue(!task.getUpdatedAt().isBefore(initialUpdatedAt));
    }

    private String findStoredStatus(String taskId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT status FROM tasks WHERE id = ?")) {
            statement.setString(1, taskId);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString(1);
            }
        }
    }
}
