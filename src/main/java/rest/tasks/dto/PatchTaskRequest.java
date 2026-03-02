package rest.tasks.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PatchTaskRequest {

    private static final String TAG_PATTERN = "^[a-z0-9][a-z0-9-_]*$";

    private JsonNode title;
    private JsonNode description;
    private JsonNode status;

    @JsonProperty("due_date")
    private JsonNode dueDate;

    private JsonNode priority;

    @JsonProperty("add_tags")
    private Set<@NotBlank @Size(min = 1, max = 64) @Pattern(regexp = TAG_PATTERN) String> addTags;

    @JsonProperty("remove_tags")
    private Set<@NotBlank @Size(min = 1, max = 64) @Pattern(regexp = TAG_PATTERN) String> removeTags;

    public boolean hasTitle() {
        return title != null;
    }

    public String titleValue() {
        return valueAsTextOrNull(title);
    }

    public boolean hasDescription() {
        return description != null;
    }

    public String descriptionValue() {
        return valueAsTextOrNull(description);
    }

    private String valueAsTextOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    public JsonNode getTitle() {
        return title;
    }

    public void setTitle(JsonNode title) {
        this.title = title;
    }

    public JsonNode getDescription() {
        return description;
    }

    public void setDescription(JsonNode description) {
        this.description = description;
    }

    public JsonNode getStatus() {
        return status;
    }

    public void setStatus(JsonNode status) {
        this.status = status;
    }

    public JsonNode getDueDate() {
        return dueDate;
    }

    public void setDueDate(JsonNode dueDate) {
        this.dueDate = dueDate;
    }

    public JsonNode getPriority() {
        return priority;
    }

    public void setPriority(JsonNode priority) {
        this.priority = priority;
    }

    public Set<String> getAddTags() {
        return addTags;
    }

    public void setAddTags(Set<String> addTags) {
        this.addTags = addTags;
    }

    public Set<String> getRemoveTags() {
        return removeTags;
    }

    public void setRemoveTags(Set<String> removeTags) {
        this.removeTags = removeTags;
    }
}
