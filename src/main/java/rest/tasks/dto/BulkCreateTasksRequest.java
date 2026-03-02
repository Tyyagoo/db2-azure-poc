package rest.tasks.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public class BulkCreateTasksRequest {

    @NotEmpty
    private List<@Valid CreateTaskRequest> tasks;

    public List<CreateTaskRequest> getTasks() {
        return tasks;
    }

    public void setTasks(List<CreateTaskRequest> tasks) {
        this.tasks = tasks;
    }
}
