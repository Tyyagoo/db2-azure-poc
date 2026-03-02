package rest;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.CheckedTemplate;
import io.quarkiverse.renarde.Controller;
import model.task.TaskEntity;
import model.task.TaskStatus;
import rest.tasks.dto.CreateTaskRequest;
import service.TaskNotFoundException;
import service.TaskService;
import service.task.TaskQuery;

/**
 * This defines a REST controller, each method will be available under the "Classname/method" URI by convention
 */
@Path("/")
public class Todos extends Controller {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Inject
    TaskService taskService;
    
    /**
     * This defines templates available in src/main/resources/templates/Classname/method.html by convention
     */
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance todos(List<TodoView> todos);
    }

    @Path("/")
    public TemplateInstance index() {
        List<TodoView> todos = taskService.list(TaskQuery.defaults().withSize(200)).items().stream()
                .map(TodoView::fromEntity)
                .toList();
        return Templates.todos(todos);
    }
    
    @POST
    public void add(@RestForm @NotBlank @Size(max = 255) String title) {
        if (validationFailed()) {
            index();
        }

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(title);
        request.setStatus(TaskStatus.OPEN);
        taskService.create(request);

        index();
    }

    @POST
    @Path("/complete")
    public void complete(@RestForm @NotBlank String id) {
        runIgnoringMissing(() -> taskService.markCompleted(id));
        index();
    }

    @POST
    @Path("/reopen")
    public void reopen(@RestForm @NotBlank String id) {
        runIgnoringMissing(() -> taskService.reopen(id));
        index();
    }

    @POST
    @Path("/delete")
    public void delete(@RestForm @NotBlank String id) {
        runIgnoringMissing(() -> taskService.softDelete(id));
        index();
    }

    private void runIgnoringMissing(Runnable action) {
        try {
            action.run();
        } catch (TaskNotFoundException ignored) {
            // Missing task should not block a post-redirect-get flow.
        }
    }

    public record TodoView(
            String id,
            String title,
            boolean completed,
            String completedAt,
            String dueDate,
            Short priority,
            List<String> tags) {

        static TodoView fromEntity(TaskEntity entity) {
            String completedAt = null;
            if (entity.getStatus() == TaskStatus.COMPLETED) {
                completedAt = DATE_FORMAT.format(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
            }

            String dueDate = null;
            if (entity.getDueDate() != null) {
                dueDate = DATE_FORMAT.format(entity.getDueDate().atOffset(ZoneOffset.UTC));
            }

            List<String> tags = entity.getTags().stream()
                    .map(tag -> tag.getName())
                    .sorted()
                    .toList();

            return new TodoView(
                    entity.getId(),
                    entity.getTitle(),
                    entity.getStatus() == TaskStatus.COMPLETED,
                    completedAt,
                    dueDate,
                    entity.getPriority(),
                    tags);
        }
    }
}
