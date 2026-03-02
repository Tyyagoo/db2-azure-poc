package service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.task.TagEntity;
import model.task.TaskAuditEntity;
import model.task.TaskEntity;
import model.task.TaskStatus;
import rest.tasks.dto.CreateTaskRequest;
import rest.tasks.dto.PatchTaskRequest;
import rest.tasks.dto.PutTaskRequest;
import rest.tasks.dto.TaskResponse;
import service.task.TaskQuery;
import service.task.TaskQueryRepository;
import service.task.TaskQueryResult;

@ApplicationScoped
public class TaskService {

    @Inject
    EntityManager entityManager;

    @Inject
    TaskQueryRepository queryRepository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public TaskEntity create(CreateTaskRequest request) {
        TaskEntity task = new TaskEntity();
        applyCreate(task, request);
        entityManager.persist(task);
        entityManager.flush();
        writeAudit(task.getId(), "create", null, toSnapshot(task));
        return task;
    }

    @Transactional
    public List<TaskEntity> bulkCreate(List<CreateTaskRequest> requests) {
        List<TaskEntity> created = new ArrayList<>();
        for (CreateTaskRequest request : requests) {
            TaskEntity task = new TaskEntity();
            applyCreate(task, request);
            entityManager.persist(task);
            created.add(task);
        }
        entityManager.flush();

        for (TaskEntity task : created) {
            writeAudit(task.getId(), "create", null, toSnapshot(task));
        }
        return created;
    }

    public TaskEntity getById(String id, boolean includeDeleted) {
        TaskEntity task = entityManager.find(TaskEntity.class, id);
        if (task == null || (!includeDeleted && task.isDeleted())) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    public TaskQueryResult list(TaskQuery query) {
        return queryRepository.search(query);
    }

    @Transactional
    public TaskEntity update(String id, PutTaskRequest request) {
        TaskEntity task = getById(id, true);
        String before = toSnapshot(task);

        task.setTitle(trimToNull(request.getTitle()));
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() == null ? TaskStatus.OPEN : request.getStatus());
        task.setDueDate(toInstant(request.getDueDate()));
        task.setPriority(request.getPriority());
        task.setTags(resolveTags(request.getTags()));

        entityManager.flush();
        writeAudit(task.getId(), "update", before, toSnapshot(task));
        return task;
    }

    @Transactional
    public TaskEntity patch(String id, PatchTaskRequest request) {
        TaskEntity task = getById(id, true);
        String before = toSnapshot(task);

        if (request.hasTitle() && request.titleValue() != null) {
            task.setTitle(trimToNull(request.titleValue()));
        }
        if (request.hasDescription()) {
            task.setDescription(request.descriptionValue());
        }
        if (request.getStatus() != null && !request.getStatus().isNull()) {
            task.setStatus(parseStatus(request.getStatus()));
        }
        if (request.getDueDate() != null) {
            task.setDueDate(parseDueDate(request.getDueDate()));
        }
        if (request.getPriority() != null) {
            task.setPriority(parsePriority(request.getPriority()));
        }

        Set<TagEntity> tags = task.getTags();
        if (request.getAddTags() != null && !request.getAddTags().isEmpty()) {
            tags.addAll(resolveTags(request.getAddTags()));
        }
        if (request.getRemoveTags() != null && !request.getRemoveTags().isEmpty()) {
            Set<String> toRemove = normalizeTags(request.getRemoveTags());
            tags.removeIf(tag -> toRemove.contains(tag.getName()));
        }

        entityManager.flush();
        writeAudit(task.getId(), "update", before, toSnapshot(task));
        return task;
    }

    @Transactional
    public void softDelete(String id) {
        TaskEntity task = getById(id, true);
        String before = toSnapshot(task);
        task.setDeletedAt(Instant.now());
        entityManager.flush();
        writeAudit(task.getId(), "delete", before, toSnapshot(task));
    }

    @Transactional
    public int bulkSoftDelete(List<String> ids) {
        int affected = 0;
        for (String id : ids) {
            softDelete(id);
            affected++;
        }
        return affected;
    }

    @Transactional
    public void restore(String id) {
        TaskEntity task = getById(id, true);
        String before = toSnapshot(task);
        task.setDeletedAt(null);
        entityManager.flush();
        writeAudit(task.getId(), "restore", before, toSnapshot(task));
    }

    @Transactional
    public void hardDelete(String id) {
        TaskEntity task = getById(id, true);
        String before = toSnapshot(task);
        String taskId = task.getId();
        entityManager.remove(task);
        entityManager.flush();
        writeAudit(taskId, "hard_delete", before, null);
    }

    private void applyCreate(TaskEntity task, CreateTaskRequest request) {
        task.setTitle(trimToNull(request.getTitle()));
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() == null ? TaskStatus.OPEN : request.getStatus());
        task.setDueDate(toInstant(request.getDueDate()));
        task.setPriority(request.getPriority());
        task.setTags(resolveTags(request.getTags()));
    }

    private TaskStatus parseStatus(JsonNode statusNode) {
        return TaskStatus.fromApiValue(statusNode.asText());
    }

    private Instant parseDueDate(JsonNode dueDateNode) {
        if (dueDateNode.isNull()) {
            return null;
        }
        try {
            return toInstant(LocalDate.parse(dueDateNode.asText()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid due_date value: " + dueDateNode.asText(), e);
        }
    }

    private Short parsePriority(JsonNode priorityNode) {
        if (priorityNode.isNull()) {
            return null;
        }
        return (short) priorityNode.asInt();
    }

    private Instant toInstant(LocalDate value) {
        if (value == null) {
            return null;
        }
        return value.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Set<TagEntity> resolveTags(Set<String> rawTags) {
        Set<String> normalized = normalizeTags(rawTags);
        Set<TagEntity> resolved = new LinkedHashSet<>();

        for (String tagName : normalized) {
            TagEntity tag = entityManager.createQuery("FROM TagEntity t WHERE t.name = :name", TagEntity.class)
                    .setParameter("name", tagName)
                    .getResultStream()
                    .findFirst()
                    .orElseGet(() -> createTag(tagName));
            resolved.add(tag);
        }
        return resolved;
    }

    private TagEntity createTag(String tagName) {
        TagEntity tag = new TagEntity();
        tag.setName(tagName);
        entityManager.persist(tag);
        return tag;
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private void writeAudit(String taskId, String action, String beforeSnapshot, String afterSnapshot) {
        TaskAuditEntity audit = new TaskAuditEntity();
        audit.setTaskId(taskId);
        audit.setAction(action);
        audit.setAt(Instant.now());
        audit.setBeforeSnapshot(beforeSnapshot);
        audit.setAfterSnapshot(afterSnapshot);
        entityManager.persist(audit);
    }

    private String toSnapshot(TaskEntity task) {
        try {
            return objectMapper.writeValueAsString(TaskResponse.fromEntity(task));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize task snapshot", e);
        }
    }
}
