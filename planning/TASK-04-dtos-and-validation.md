# TASK-04: DTOs + Validation (Create/Update/Patch/Bulk)

Goal

Define REST request/response models with validation rules and PATCH semantics.

Deliverables

Suggested DTO package:

- `src/main/java/rest/tasks/dto/CreateTaskRequest.java`
- `src/main/java/rest/tasks/dto/BulkCreateTasksRequest.java`
- `src/main/java/rest/tasks/dto/PutTaskRequest.java`
- `src/main/java/rest/tasks/dto/PatchTaskRequest.java`
- `src/main/java/rest/tasks/dto/TaskResponse.java`
- `src/main/java/rest/tasks/dto/TaskListResponse.java` (optional; or return a plain list + pagination headers)

Validation rules (Bean Validation)

- `title`: required, not blank, max length (match DB, e.g. 200)
- `description`: optional, max length if you want (DB CLOB is large; keep API reasonable)
- `status`: optional on create (default OPEN), validated to allowed values
- `due_date`: optional, ISO-8601 date
- `priority`: optional; define a reasonable range (e.g. 1..5) or allow smallint
- `tags`: optional; each tag 1..64 chars, pattern `[a-z0-9][a-z0-9-_]*` (pick something stable)

PATCH semantics

PATCH must support partial updates and clearing optional fields.

Recommended implementation approach:

- Represent PATCH as a DTO with `Optional<T>` fields OR use Jackson `JsonNode` and apply a patch interpreter.
- Requirement: must distinguish:
  - missing field (do nothing)
  - present with null (clear field)

Tags in PATCH

Support additive/removal operations without requiring the entire tag list:

- `add_tags`: list
- `remove_tags`: list

Acceptance criteria

- DTOs compile.
- Validation annotations are in place.
- PATCH structure can express:
  - update title only
  - clear description
  - add/remove tags

Notes

- Keep API field names snake_case to match the requirement (`due_date`, `created_at`, etc.) or decide on camelCase and document it. If you choose camelCase, ensure it is consistent across all DTOs.
