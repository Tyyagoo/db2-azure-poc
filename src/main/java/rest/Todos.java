package rest;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.hibernate.exception.JDBCConnectionException;

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
        public static native TemplateInstance todos(List<TodoView> todos, String boardError, String toastMessage);
    }

    @Path("/")
    public TemplateInstance index() {
        BoardData boardData = loadBoardData();
        String toastMessage = this.flash.get("toastError");
        return Templates.todos(boardData.todos(), boardData.boardError(), toastMessage);
    }
    
    @POST
    public void add(@RestForm @NotBlank @Size(max = 255) String title) {
        if (validationFailed()) {
            index();
        }

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(title);
        request.setStatus(TaskStatus.OPEN);
        try {
            taskService.create(request);
        } catch (RuntimeException e) {
            if (isDatabaseUnavailable(e)) {
                flash("toastError", "Could not add task: database is currently unavailable.");
                index();
            }
            throw e;
        }

        index();
    }

    @POST
    @Path("/complete")
    public void complete(@RestForm @NotBlank String id) {
        runIgnoringMissing(
                () -> taskService.markCompleted(id),
                "Could not complete task: database is currently unavailable.");
        index();
    }

    @POST
    @Path("/reopen")
    public void reopen(@RestForm @NotBlank String id) {
        runIgnoringMissing(
                () -> taskService.reopen(id),
                "Could not reopen task: database is currently unavailable.");
        index();
    }

    @POST
    @Path("/delete")
    public void delete(@RestForm @NotBlank String id) {
        runIgnoringMissing(
                () -> taskService.softDelete(id),
                "Could not delete task: database is currently unavailable.");
        index();
    }

    private BoardData loadBoardData() {
        try {
            List<TodoView> todos = taskService.list(TaskQuery.defaults().withSize(200)).items().stream()
                    .map(TodoView::fromEntity)
                    .toList();
            return new BoardData(todos, null);
        } catch (RuntimeException e) {
            if (isDatabaseUnavailable(e)) {
                return new BoardData(
                        Collections.emptyList(),
                        "Database is unavailable right now. Please check DB2 connectivity and try again.");
            }
            throw e;
        }
    }

    private void runIgnoringMissing(Runnable action, String unavailableMessage) {
        try {
            action.run();
        } catch (TaskNotFoundException ignored) {
            // Missing task should not block a post-redirect-get flow.
        } catch (RuntimeException e) {
            if (isDatabaseUnavailable(e)) {
                flash("toastError", unavailableMessage);
                return;
            }
            throw e;
        }
    }

    static boolean isDatabaseUnavailable(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof JDBCConnectionException
                    || cursor instanceof SQLTransientConnectionException
                    || cursor instanceof SQLNonTransientConnectionException) {
                return true;
            }

            if (cursor instanceof SQLException sqlException) {
                String state = sqlException.getSQLState();
                if (state != null && state.startsWith("08")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private record BoardData(List<TodoView> todos, String boardError) {
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
