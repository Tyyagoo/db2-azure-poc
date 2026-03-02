package service;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import model.task.TaskAuditEntity;
import model.task.TaskEntity;
import model.task.TaskStatus;
import rest.tasks.dto.CreateTaskRequest;
import rest.tasks.dto.PatchTaskRequest;
import rest.tasks.dto.PutTaskRequest;
import service.task.TaskQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TaskServiceTest {

    @Inject
    TaskService taskService;

    @Inject
    EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @TestTransaction
    void shouldCreateTaskWithNormalizedTagsAndAudit() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("  Write docs  ");
        request.setDescription("Intro");
        request.setTags(Set.of(" Docs", "db2 "));

        TaskEntity created = taskService.create(request);

        assertNotNull(created.getId());
        assertEquals(TaskStatus.OPEN, created.getStatus());
        assertEquals(2, created.getTags().size());
        assertTrue(created.getTags().stream().anyMatch(tag -> tag.getName().equals("docs")));
        assertTrue(created.getTags().stream().anyMatch(tag -> tag.getName().equals("db2")));

        List<TaskAuditEntity> audits = findAudits(created.getId());
        assertEquals(1, audits.size());
        assertEquals("create", audits.get(0).getAction());
        assertNull(audits.get(0).getBeforeSnapshot());
        assertNotNull(audits.get(0).getAfterSnapshot());
    }

    @Test
    @TestTransaction
    void shouldBulkCreateAllOrNothingAndListViaQueryModel() {
        CreateTaskRequest first = new CreateTaskRequest();
        first.setTitle("Bulk one");
        first.setTags(Set.of("Ops"));

        CreateTaskRequest second = new CreateTaskRequest();
        second.setTitle("Bulk two");
        second.setStatus(TaskStatus.COMPLETED);

        List<TaskEntity> created = taskService.bulkCreate(List.of(first, second));

        assertEquals(2, created.size());
        assertEquals(2, taskService.list(TaskQuery.defaults()).items().size());
    }

    @Test
    @TestTransaction
    void shouldReplaceAndPatchTaskAndWriteUpdateAudits() throws Exception {
        TaskEntity original = taskService.create(createRequest("Original", Set.of("one")));

        PutTaskRequest put = new PutTaskRequest();
        put.setTitle("Replaced");
        put.setDescription("After put");
        put.setStatus(TaskStatus.COMPLETED);
        put.setTags(Set.of("two"));

        TaskEntity replaced = taskService.update(original.getId(), put);
        assertEquals("Replaced", replaced.getTitle());
        assertEquals(TaskStatus.COMPLETED, replaced.getStatus());
        assertEquals(1, replaced.getTags().size());
        assertTrue(replaced.getTags().stream().anyMatch(tag -> tag.getName().equals("two")));

        PatchTaskRequest patch = objectMapper.readValue(
                "{\"description\":null,\"priority\":null,\"add_tags\":[\"three\"],\"remove_tags\":[\"two\"]}",
                PatchTaskRequest.class);

        TaskEntity patched = taskService.patch(original.getId(), patch);
        assertNull(patched.getDescription());
        assertNull(patched.getPriority());
        assertTrue(patched.getTags().stream().anyMatch(tag -> tag.getName().equals("three")));
        assertFalse(patched.getTags().stream().anyMatch(tag -> tag.getName().equals("two")));

        List<TaskAuditEntity> audits = findAudits(original.getId());
        assertEquals(3, audits.size());
        assertEquals("update", audits.get(1).getAction());
        assertEquals("update", audits.get(2).getAction());
    }

    @Test
    @TestTransaction
    void shouldSoftDeleteRestoreAndHardDeleteWithAudit() {
        TaskEntity created = taskService.create(createRequest("Lifecycle", Set.of("lifecycle")));

        taskService.softDelete(created.getId());
        assertTrue(taskService.getById(created.getId(), true).isDeleted());

        taskService.restore(created.getId());
        assertFalse(taskService.getById(created.getId(), false).isDeleted());

        taskService.hardDelete(created.getId());
        assertTrue(entityManager.find(TaskEntity.class, created.getId()) == null);

        List<TaskAuditEntity> audits = findAudits(created.getId());
        assertEquals(4, audits.size());
        assertEquals("delete", audits.get(1).getAction());
        assertEquals("restore", audits.get(2).getAction());
        assertEquals("hard_delete", audits.get(3).getAction());
    }

    @Test
    @TestTransaction
    void shouldSoftDeleteInBulk() {
        TaskEntity first = taskService.create(createRequest("A", Set.of("x")));
        TaskEntity second = taskService.create(createRequest("B", Set.of("y")));

        int affected = taskService.bulkSoftDelete(List.of(first.getId(), second.getId()));

        assertEquals(2, affected);
        assertTrue(taskService.getById(first.getId(), true).isDeleted());
        assertTrue(taskService.getById(second.getId(), true).isDeleted());
    }

    private CreateTaskRequest createRequest(String title, Set<String> tags) {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(title);
        request.setTags(tags);
        return request;
    }

    private List<TaskAuditEntity> findAudits(String taskId) {
        return entityManager.createQuery(
                        "FROM TaskAuditEntity a WHERE a.taskId = :taskId ORDER BY a.id ASC",
                        TaskAuditEntity.class)
                .setParameter("taskId", taskId)
                .getResultList();
    }
}
