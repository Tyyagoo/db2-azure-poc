# TASK-07: REST API (Endpoints + ETag/If-Match)

Goal

Expose the service layer via a REST API matching the required endpoints, including concurrency protection.

Deliverables

- `src/main/java/rest/tasks/TaskResource.java`

Endpoints

Required:

- `POST /tasks`
- `GET /tasks`
- `GET /tasks/{id}`
- `PUT /tasks/{id}`
- `PATCH /tasks/{id}`
- `DELETE /tasks/{id}`

Additional endpoints (needed for requirements):

- `POST /tasks/bulk`
- `DELETE /tasks/bulk`
- `POST /tasks/{id}/restore`
- Optional: `GET /tasks/trash` (or implement `GET /tasks?deleted=true` only)

HTTP semantics

Create

- `POST /tasks` returns 201 + body with created task.
- Bulk create returns 201 + list.

Read

- `GET /tasks` returns 200 + paginated results.
- `GET /tasks/{id}` returns 200.

Update

- PUT/PATCH return 200 + updated task.

Delete

- Soft delete default.
- Hard delete if `?hard=true`.
- Return 204 or 200 (pick one; keep consistent).

Concurrency protection

- Use optimistic locking via entity `version`.
- `GET /tasks/{id}` returns `ETag: W/"<version>"`.
- `PUT/PATCH/DELETE` require `If-Match: W/"<version>"`.
  - If missing: 428 Precondition Required
  - If mismatch: 412 Precondition Failed

Implementation notes

- ETag parsing should accept both `W/"n"` and `"n"`.
- Compare to current entity version.
- On update, the returned ETag should reflect the new version.

Acceptance criteria

- All endpoints exist and wire to service methods.
- Query params map to service list/search/filter/sort/pagination.
- ETag/If-Match enforced for write operations.
