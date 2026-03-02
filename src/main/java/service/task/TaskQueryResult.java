package service.task;

import java.util.List;

import model.task.TaskEntity;

public record TaskQueryResult(List<TaskEntity> items, long total, int page, int size) {
}
