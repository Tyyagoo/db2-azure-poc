package rest.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import model.task.TaskEntity;
import model.task.TaskStatus;
import rest.tasks.dto.BulkCreateTasksRequest;
import rest.tasks.dto.BulkDeleteTasksRequest;
import rest.tasks.dto.CreateTaskRequest;
import rest.tasks.dto.PatchTaskRequest;
import rest.tasks.dto.PutTaskRequest;
import rest.tasks.dto.TaskListResponse;
import rest.tasks.dto.TaskResponse;
import service.TaskNotFoundException;
import service.TaskService;
import service.task.SortOrder;
import service.task.TaskQuery;
import service.task.TaskQueryResult;
import service.task.TaskSort;

@Path("/tasks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {

    @Inject
    TaskService taskService;

    @POST
    public Response create(@Valid CreateTaskRequest request) {
        TaskEntity created = taskService.create(request);
        return responseWithEtag(Response.Status.CREATED, TaskResponse.fromEntity(created), created.getVersion());
    }

    @POST
    @Path("/bulk")
    public Response bulkCreate(@Valid BulkCreateTasksRequest request) {
        List<TaskResponse> created = taskService.bulkCreate(request.getTasks()).stream().map(TaskResponse::fromEntity).toList();
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    public TaskListResponse list(
            @QueryParam("q") String keyword,
            @QueryParam("status") String status,
            @QueryParam("tag") List<String> tags,
            @DefaultValue("newest") @QueryParam("sort") String sort,
            @DefaultValue("desc") @QueryParam("order") String order,
            @DefaultValue("1") @QueryParam("page") int page,
            @DefaultValue("20") @QueryParam("size") int size,
            @DefaultValue("false") @QueryParam("deleted") boolean deleted) {

        TaskQuery query = TaskQuery.defaults()
                .withKeyword(keyword)
                .withStatus(parseStatus(status))
                .withTags(parseTags(tags))
                .withSort(parseSort(sort))
                .withOrder(parseOrder(order))
                .withPage(page)
                .withSize(size)
                .withDeleted(deleted);

        TaskQueryResult result = taskService.list(query);
        TaskListResponse response = new TaskListResponse();
        response.setItems(result.items().stream().map(TaskResponse::fromEntity).toList());
        response.setTotal(result.total());
        response.setPage(result.page());
        response.setPageSize(result.size());
        return response;
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TaskEntity task = findOrThrowNotFound(() -> taskService.getById(id, false));
        return responseWithEtag(Response.Status.OK, TaskResponse.fromEntity(task), task.getVersion());
    }

    @PUT
    @Path("/{id}")
    public Response update(
            @PathParam("id") String id,
            @HeaderParam("If-Match") String ifMatch,
            @Valid PutTaskRequest request) {
        TaskEntity current = findOrThrowNotFound(() -> taskService.getById(id, true));
        ensureIfMatch(ifMatch, current.getVersion());

        TaskEntity updated = findOrThrowNotFound(() -> taskService.update(id, request));
        return responseWithEtag(Response.Status.OK, TaskResponse.fromEntity(updated), updated.getVersion());
    }

    @PATCH
    @Path("/{id}")
    public Response patch(
            @PathParam("id") String id,
            @HeaderParam("If-Match") String ifMatch,
            @Valid PatchTaskRequest request) {
        TaskEntity current = findOrThrowNotFound(() -> taskService.getById(id, true));
        ensureIfMatch(ifMatch, current.getVersion());

        TaskEntity updated = findOrThrowNotFound(() -> taskService.patch(id, request));
        return responseWithEtag(Response.Status.OK, TaskResponse.fromEntity(updated), updated.getVersion());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(
            @PathParam("id") String id,
            @HeaderParam("If-Match") String ifMatch,
            @DefaultValue("false") @QueryParam("hard") boolean hardDelete) {
        TaskEntity current = findOrThrowNotFound(() -> taskService.getById(id, true));
        ensureIfMatch(ifMatch, current.getVersion());

        if (hardDelete) {
            findOrThrowNotFoundVoid(() -> taskService.hardDelete(id));
        } else {
            findOrThrowNotFoundVoid(() -> taskService.softDelete(id));
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/bulk")
    public Response bulkDelete(@Valid BulkDeleteTasksRequest request) {
        int deleted = taskService.bulkSoftDelete(request.getIds());
        return Response.ok(Map.of("deleted", deleted)).build();
    }

    @POST
    @Path("/{id}/restore")
    @Consumes(MediaType.WILDCARD)
    public Response restore(@PathParam("id") String id) {
        findOrThrowNotFoundVoid(() -> taskService.restore(id));
        TaskEntity restored = findOrThrowNotFound(() -> taskService.getById(id, false));
        return responseWithEtag(Response.Status.OK, TaskResponse.fromEntity(restored), restored.getVersion());
    }

    private TaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TaskStatus.fromApiValue(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }
    }

    private TaskSort parseSort(String sort) {
        if (sort == null || sort.isBlank() || "newest".equalsIgnoreCase(sort)) {
            return TaskSort.NEWEST;
        }
        if ("due_date".equalsIgnoreCase(sort)) {
            return TaskSort.DUE_DATE;
        }
        throw new BadRequestException("Invalid sort: " + sort);
    }

    private SortOrder parseOrder(String order) {
        if (order == null || order.isBlank()) {
            return SortOrder.DESC;
        }
        String normalized = order.toUpperCase(Locale.ROOT);
        try {
            return SortOrder.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order: " + order);
        }
    }

    private java.util.Set<String> parseTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return java.util.Set.of();
        }

        List<String> values = new ArrayList<>();
        for (String rawTag : rawTags) {
            if (rawTag == null) {
                continue;
            }
            String[] split = rawTag.split(",");
            for (String token : split) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
        }
        return new java.util.LinkedHashSet<>(values);
    }

    private void ensureIfMatch(String ifMatch, Long currentVersion) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new WebApplicationException(Response.status(428).build());
        }

        Long requestedVersion = parseIfMatchVersion(ifMatch);
        if (!currentVersion.equals(requestedVersion)) {
            throw new WebApplicationException(Response.Status.PRECONDITION_FAILED);
        }
    }

    private Long parseIfMatchVersion(String ifMatch) {
        String candidate = ifMatch.trim();
        if (candidate.startsWith("W/")) {
            candidate = candidate.substring(2).trim();
        }
        if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() >= 2) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }

        try {
            return Long.parseLong(candidate);
        } catch (NumberFormatException e) {
            throw new WebApplicationException(Response.Status.PRECONDITION_FAILED);
        }
    }

    private Response responseWithEtag(Response.Status status, TaskResponse body, Long version) {
        EntityTag eTag = new EntityTag(String.valueOf(version), true);
        return Response.status(status).entity(body).tag(eTag).build();
    }

    private TaskEntity findOrThrowNotFound(TaskSupplier supplier) {
        try {
            return supplier.get();
        } catch (TaskNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    private void findOrThrowNotFoundVoid(TaskRunnable runnable) {
        try {
            runnable.run();
        } catch (TaskNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @FunctionalInterface
    private interface TaskSupplier {
        TaskEntity get();
    }

    @FunctionalInterface
    private interface TaskRunnable {
        void run();
    }
}
