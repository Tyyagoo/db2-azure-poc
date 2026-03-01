# TASK-08: Error Handling (400/404 + Preconditions)

Goal

Provide clear, consistent error responses:

- 400 for invalid inputs and validation failures
- 404 for missing resources
- 412/428 for concurrency preconditions

Deliverables

Suggested implementation:

- Custom exceptions in `src/main/java/rest/tasks/errors/`
  - `NotFoundException` (domain-level)
  - `PreconditionRequiredException`
  - `PreconditionFailedException`

- Exception mappers in `src/main/java/rest/tasks/errors/`
  - `NotFoundExceptionMapper`
  - `PreconditionRequiredExceptionMapper`
  - `PreconditionFailedExceptionMapper`
  - A mapper for validation errors if needed (Quarkus already provides some; standardize format if desired)

Error response format (proposed)

Return JSON:

```
{
  "error": "not_found",
  "message": "Task not found",
  "details": { ... }
}
```

Guidelines

- Prefer stable machine-readable `error` codes.
- Don’t leak internal SQL/stack traces.
- Keep 404 responses the same whether soft-deleted tasks are hidden or not.

Acceptance criteria

- Invalid requests return 400 with useful messages.
- Missing id returns 404.
- Missing `If-Match` returns 428.
- Stale `If-Match` returns 412.
