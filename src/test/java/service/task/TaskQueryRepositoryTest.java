package service.task;

import java.time.Instant;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import model.task.TagEntity;
import model.task.TaskEntity;
import model.task.TaskStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TaskQueryRepositoryTest {

    @Inject
    EntityManager entityManager;

    @Inject
    TaskQueryRepository repository;

    @Test
    @TestTransaction
    void shouldExcludeDeletedByDefaultAndReturnNewestFirst() {
        String scopedTag = "qrepo-default-scope";
        TaskEntity old = persistTask("Old", "desc", TaskStatus.OPEN, Instant.parse("2026-01-01T00:00:00Z"), null, Set.of(scopedTag));
        TaskEntity newest = persistTask("Newest", "desc", TaskStatus.OPEN, Instant.parse("2026-01-03T00:00:00Z"), null, Set.of(scopedTag));
        persistTask("Deleted", "desc", TaskStatus.OPEN, Instant.parse("2026-01-02T00:00:00Z"), Instant.parse("2026-01-04T00:00:00Z"), Set.of(scopedTag));

        TaskQuery query = TaskQuery.defaults().withTags(Set.of(scopedTag));

        TaskQueryResult result = repository.search(query);

        assertEquals(2, result.total());
        assertEquals(2, result.items().size());
        assertEquals(newest.getId(), result.items().get(0).getId());
        assertEquals(old.getId(), result.items().get(1).getId());
    }

    @Test
    @TestTransaction
    void shouldFilterByDeletedKeywordStatusAndTags() {
        String scopedTag = "qrepo-filter-scope";
        persistTask("Prepare docs", "Release note", TaskStatus.OPEN, Instant.parse("2026-02-01T00:00:00Z"), null, Set.of("docs", scopedTag));
        TaskEntity deletedCompleted = persistTask("Deploy", "Production release", TaskStatus.COMPLETED,
                Instant.parse("2026-02-02T00:00:00Z"), Instant.parse("2026-02-03T00:00:00Z"), Set.of("release", scopedTag));

        TaskQuery query = TaskQuery.defaults()
                .withDeleted(true)
                .withKeyword("production")
                .withStatus(TaskStatus.COMPLETED)
                .withTags(Set.of(scopedTag));

        TaskQueryResult result = repository.search(query);

        assertEquals(1, result.total());
        assertEquals(deletedCompleted.getId(), result.items().get(0).getId());
    }

    @Test
    @TestTransaction
    void shouldApplyDueDateSortingAndPagination() {
        String scopedTag = "qrepo-page-scope";
        TaskEntity firstDue = persistTask("A", "x", TaskStatus.OPEN, Instant.parse("2026-03-01T00:00:00Z"), null, Set.of(scopedTag));
        TaskEntity secondDue = persistTask("B", "x", TaskStatus.OPEN, Instant.parse("2026-03-02T00:00:00Z"), null, Set.of(scopedTag));
        persistTask("C", "x", TaskStatus.OPEN, Instant.parse("2026-03-03T00:00:00Z"), null, Set.of(scopedTag));

        TaskQuery query = TaskQuery.defaults()
                .withSort(TaskSort.DUE_DATE)
                .withOrder(SortOrder.ASC)
                .withTags(Set.of(scopedTag))
                .withPage(2)
                .withSize(1);

        TaskQueryResult result = repository.search(query);

        assertEquals(3, result.total());
        assertEquals(1, result.items().size());
        assertTrue(result.page() == 2);
        assertTrue(result.size() == 1);
        assertEquals(secondDue.getId(), result.items().get(0).getId());

        TaskQuery firstPage = query.withPage(1);
        TaskQueryResult firstPageResult = repository.search(firstPage);
        assertEquals(firstDue.getId(), firstPageResult.items().get(0).getId());
    }

    private TaskEntity persistTask(String title, String description, TaskStatus status, Instant dueDate, Instant deletedAt, Set<String> tags) {
        TaskEntity task = new TaskEntity();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setDeletedAt(deletedAt);
        task.setTags(tags.stream().map(this::persistTag).collect(java.util.stream.Collectors.toSet()));
        entityManager.persist(task);
        entityManager.flush();
        return task;
    }

    private TagEntity persistTag(String name) {
        TagEntity existing = entityManager.createQuery("FROM TagEntity t WHERE t.name = :name", TagEntity.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (existing != null) {
            return existing;
        }

        TagEntity tag = new TagEntity();
        tag.setName(name);
        entityManager.persist(tag);
        entityManager.flush();
        return tag;
    }
}
