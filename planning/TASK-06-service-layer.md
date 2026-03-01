# TASK-06: Service Layer (CRUD + Bulk + Tags + Trash/Restore + Audit)

Goal

Implement the core business logic in a service class that talks to the database.

Deliverables

- `src/main/java/service/TaskService.java`
- Any small helper classes needed for query/pagination results

Required operations

Create

- Create a single task
  - default status = OPEN
  - tags are optional; normalize tags by:
    - trimming
    - lowercasing (if chosen)
    - upserting into `tags`
    - creating rows in `task_tags`
- Bulk create
  - accept a list of create requests
  - transaction behavior: either all-or-nothing or best-effort; pick one and document
  - recommendation: all-or-nothing for simplicity

Read

- Get by id
  - default excludes soft-deleted unless explicitly requested
- List/search/filter/sort/paginate (from TASK-05)

Update

- Full update (PUT)
  - replace all updatable fields
  - tags: treat provided tags list as authoritative
- Partial update (PATCH)
  - apply only provided fields
  - support clearing optional fields (description, due_date, priority)
  - support `add_tags` / `remove_tags`

Delete

- Soft delete
  - set `deleted_at` (and update `updated_at`)
- Bulk soft delete
- Restore
  - set `deleted_at = NULL`
- Optional hard delete
  - delete from join table then tasks row (or rely on FK cascading if defined)

Audit/history

Write a `task_audit` row for each change:

- CREATE: after snapshot
- UPDATE/PATCH: before + after snapshots
- SOFT_DELETE: before + after
- RESTORE: before + after
- HARD_DELETE: before snapshot only

Snapshot format

- Use JSON string; simplest is to serialize the response DTO or a map of fields.
- Keep snapshots stable and not excessively large.

Transactions

- Use `@Transactional` on service methods that write.
- Reads can be non-transactional.

Acceptance criteria

- Service methods exist for all required operations.
- Soft delete, restore, hard delete work.
- Tag normalization/upsert works.
- Audit rows are written for each write operation.

Notes

- Since auth is out of scope, set `actor` to a fixed value (e.g. "system") or accept an optional header later.
