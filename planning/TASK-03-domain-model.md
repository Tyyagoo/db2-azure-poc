# TASK-03: Domain Model (Entities + Enums + Soft Delete + Version)

Goal

Implement the JPA/Panache entities that map to the Flyway schema.

Deliverables

Create new Java types (suggested packages; adjust to repo conventions):

- `src/main/java/model/task/TaskStatus.java`
- `src/main/java/model/task/TaskEntity.java`
- `src/main/java/model/task/TagEntity.java`
- `src/main/java/model/task/TaskAuditEntity.java`

Entity requirements

TaskEntity

- Fields: `id`, `title`, `description`, `status`, `dueDate`, `priority`, `createdAt`, `updatedAt`, `deletedAt`, `version`
- Tags relationship (many-to-many via `task_tags`)
- Soft delete: `deletedAt != null` means deleted
- Optimistic lock: `@Version` maps to `version`
- Timestamp handling:
  - `@PrePersist`: set `createdAt` and `updatedAt` if null
  - `@PreUpdate`: set `updatedAt`

TagEntity

- `id`, `name`
- Unique `name` enforced by DB

TaskAuditEntity

- `id`, `taskId`, `action`, `actor` (for now actor can be a fixed value like "system" since auth is out of scope), `at`, `beforeSnapshot`, `afterSnapshot`

Implementation steps

1) Create `TaskStatus` enum

- Values: `OPEN`, `COMPLETED`
- Provide mapping to/from API values `open`/`completed` (either via Jackson annotations or DTO mapping).

2) Implement entities

- Prefer `PanacheEntityBase` (since `id` is not numeric by default).
- Map UUID ids as `String` (`CHAR(36)` in DB) or `UUID` if you prefer (ensure Db2/H2 column mapping works).
- Keep field access consistent (private + getters/setters is fine; Panache also supports public fields, but prefer encapsulation for new code).

3) Create minimal tests (optional in this task)

If you want quick feedback before service/resource work:

- Add a Quarkus test that persists a Task and verifies timestamps/version are set.

Acceptance criteria

- Entities compile.
- Schema column names align exactly with Flyway.
- Soft delete and version fields exist and are mapped.

Notes

- Tags are normalized. Do not store arrays in a single column.
- Don’t implement REST endpoints here.
