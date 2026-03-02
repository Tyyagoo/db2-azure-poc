package util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import model.task.TaskEntity;
import model.task.TaskStatus;
import rest.tasks.dto.CreateTaskRequest;
import service.TaskService;

@ApplicationScoped
public class Startup {

    @Inject
    TaskService taskService;

    /**
     * This method is executed at the start of your application
     */
    public void start(@Observes StartupEvent evt) {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT && TaskEntity.count() == 0) {
            CreateTaskRequest open = new CreateTaskRequest();
            open.setTitle("Review DB2 migration output");
            taskService.create(open);

            CreateTaskRequest completed = new CreateTaskRequest();
            completed.setTitle("Set up neobrutalist board theme");
            completed.setStatus(TaskStatus.COMPLETED);
            taskService.create(completed);
        }
    }
}
