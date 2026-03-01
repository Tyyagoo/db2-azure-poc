# TASK-09: Tests (QuarkusTest + RestAssured + Flyway/H2)

Goal

Create a test suite that drives and verifies the behavior of the Task CRUD API end-to-end (resource -> service -> DB).

Test framework

- Use `@QuarkusTest`.
- Use RestAssured for HTTP-level assertions.
- Use H2 `%test` datasource with Flyway migrations.

Deliverables

- `src/test/java/rest/tasks/TaskResourceTest.java`
- Optional: test utilities for JSON parsing, ETag helpers

Test cases (minimum set)

Create

- Create task with title only -> defaults: status=open, no tags
- Create task with tags/status/due_date/priority
- Bulk create -> returns list and persists all

Read

- List default -> newest first
- Pagination -> page/size works, total correct
- Search `q` hits title and description
- Filter by status
- Filter by tag
- Trash view -> deleted tasks only when requested

Update

- PUT replaces fields
- PATCH updates title only
- PATCH clears description (explicit null)
- PATCH add_tags/remove_tags

Delete

- Soft delete hides from default list
- Restore brings it back
- Bulk delete soft deletes all
- Hard delete removes permanently (and tag joins cleaned)

Concurrency

- GET returns ETag
- PUT/PATCH/DELETE without If-Match -> 428
- PUT/PATCH/DELETE with stale If-Match -> 412
- With correct If-Match -> succeeds and increments version

Audit

- Create/update/delete/restore produces a row in `task_audit`
- Snapshot fields are non-empty where expected

Commands

- Run single class: `\.\mvnw.cmd -Dtest=TaskResourceTest test`
- Full tests: `\.\mvnw.cmd test`

Acceptance criteria

- Tests pass in CI without Db2.
- Tests are deterministic and do not rely on time ordering except where explicitly controlled.
