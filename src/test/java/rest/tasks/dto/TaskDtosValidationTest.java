package rest.tasks.dto;

import java.util.List;
import java.util.Set;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskDtosValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRequireCreateTitleAndEnforceConstraints() {
        CreateTaskRequest request = new CreateTaskRequest();

        assertFalse(validator.validate(request).isEmpty());

        request.setTitle(" ");
        assertFalse(validator.validate(request).isEmpty());

        request.setTitle("a".repeat(256));
        assertFalse(validator.validate(request).isEmpty());

        request.setTitle("Valid title");
        request.setPriority((short) 0);
        assertFalse(validator.validate(request).isEmpty());

        request.setPriority((short) 6);
        assertFalse(validator.validate(request).isEmpty());

        request.setPriority((short) 3);
        request.setTags(Set.of("Invalid Tag"));
        assertFalse(validator.validate(request).isEmpty());

        request.setTags(Set.of("api", "db2_migration"));
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldValidatePutRequestLikeCreate() {
        PutTaskRequest request = new PutTaskRequest();
        request.setTitle("Valid");
        request.setPriority((short) 1);
        request.setTags(Set.of("backend"));

        assertTrue(validator.validate(request).isEmpty());

        request.setTags(Set.of(""));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldValidateBulkCreatePayload() {
        BulkCreateTasksRequest request = new BulkCreateTasksRequest();
        request.setTasks(List.of());
        assertFalse(validator.validate(request).isEmpty());

        CreateTaskRequest task = new CreateTaskRequest();
        task.setTitle("Bulk task");
        request.setTasks(List.of(task));
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldDistinguishMissingAndNullInPatch() throws Exception {
        PatchTaskRequest missingDescription = objectMapper.readValue("{\"title\":\"Only title\"}", PatchTaskRequest.class);
        assertTrue(missingDescription.hasTitle());
        assertFalse(missingDescription.hasDescription());
        assertEquals("Only title", missingDescription.titleValue());

        PatchTaskRequest clearDescription = objectMapper.readValue("{\"description\":null}", PatchTaskRequest.class);
        assertTrue(clearDescription.hasDescription());
        assertNull(clearDescription.descriptionValue());

        PatchTaskRequest tagsPatch = objectMapper.readValue(
                "{\"add_tags\":[\"db2\",\"flyway\"],\"remove_tags\":[\"legacy\"]}",
                PatchTaskRequest.class);
        assertNotNull(tagsPatch.getAddTags());
        assertNotNull(tagsPatch.getRemoveTags());
        assertEquals(2, tagsPatch.getAddTags().size());
        assertEquals(1, tagsPatch.getRemoveTags().size());
    }
}
