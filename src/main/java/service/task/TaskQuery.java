package service.task;

import java.util.LinkedHashSet;
import java.util.Set;

import model.task.TaskStatus;

public record TaskQuery(
        String keyword,
        TaskStatus status,
        Set<String> tags,
        TaskSort sort,
        SortOrder order,
        int page,
        int size,
        boolean deleted) {

    public static TaskQuery defaults() {
        return new TaskQuery(null, null, Set.of(), TaskSort.NEWEST, SortOrder.DESC, 1, 20, false);
    }

    public TaskQuery normalized() {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        Set<String> normalizedTags = tags == null
                ? Set.of()
                : tags.stream()
                        .filter(tag -> tag != null && !tag.isBlank())
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new TaskQuery(
                keyword == null ? null : keyword.trim(),
                status,
                normalizedTags,
                sort == null ? TaskSort.NEWEST : sort,
                order == null ? SortOrder.DESC : order,
                normalizedPage,
                normalizedSize,
                deleted);
    }

    public int offset() {
        TaskQuery normalized = normalized();
        return (normalized.page - 1) * normalized.size;
    }

    public TaskQuery withKeyword(String value) {
        return new TaskQuery(value, status, tags, sort, order, page, size, deleted);
    }

    public TaskQuery withStatus(TaskStatus value) {
        return new TaskQuery(keyword, value, tags, sort, order, page, size, deleted);
    }

    public TaskQuery withTags(Set<String> values) {
        return new TaskQuery(keyword, status, values, sort, order, page, size, deleted);
    }

    public TaskQuery withSort(TaskSort value) {
        return new TaskQuery(keyword, status, tags, value, order, page, size, deleted);
    }

    public TaskQuery withOrder(SortOrder value) {
        return new TaskQuery(keyword, status, tags, sort, value, page, size, deleted);
    }

    public TaskQuery withPage(int value) {
        return new TaskQuery(keyword, status, tags, sort, order, value, size, deleted);
    }

    public TaskQuery withSize(int value) {
        return new TaskQuery(keyword, status, tags, sort, order, page, value, deleted);
    }

    public TaskQuery withDeleted(boolean value) {
        return new TaskQuery(keyword, status, tags, sort, order, page, size, value);
    }
}
