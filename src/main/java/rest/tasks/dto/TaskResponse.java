package rest.tasks.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import model.task.TaskEntity;
import model.task.TaskStatus;

public class TaskResponse {

    private String id;
    private String title;
    private String description;
    private TaskStatus status;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    private Short priority;
    private Set<String> tags;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("deleted_at")
    private Instant deletedAt;

    private Long version;

    public static TaskResponse fromEntity(TaskEntity task) {
        TaskResponse response = new TaskResponse();
        response.id = task.getId();
        response.title = task.getTitle();
        response.description = task.getDescription();
        response.status = task.getStatus();
        response.dueDate = task.getDueDate() == null ? null : task.getDueDate().atOffset(ZoneOffset.UTC).toLocalDate();
        response.priority = task.getPriority();
        response.tags = task.getTags() == null
                ? new TreeSet<>()
                : task.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toCollection(TreeSet::new));
        response.createdAt = task.getCreatedAt();
        response.updatedAt = task.getUpdatedAt();
        response.deletedAt = task.getDeletedAt();
        response.version = task.getVersion();
        return response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Short getPriority() {
        return priority;
    }

    public void setPriority(Short priority) {
        this.priority = priority;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
