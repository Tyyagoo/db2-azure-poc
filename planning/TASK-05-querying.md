# TASK-05: Query Model (List/Search/Filter/Sort/Pagination)

Goal

Implement the database querying needed for `GET /tasks`:

- list active tasks (default excludes deleted)
- include deleted (trash view)
- keyword search (title + description)
- filter by status
- filter by tags
- sort by due date or newest
- pagination

Deliverables

- A query helper / repository method(s) that can construct dynamic queries safely.

Suggested approach

- Keep controllers/resources thin: query parsing in resource, query building in service/repository.
- Use Panache or JPA Criteria; avoid string concatenation with user input.

API query parameters (proposed)

- `q`: keyword
- `status`: `open|completed`
- `tag`: repeated param or comma-separated
- `sort`: `due_date|newest` (default newest)
- `order`: `asc|desc` (optional)
- `page`: 1-based
- `size`: 1..100
- `deleted`: `true|false` (default false)

Db query semantics

- Keyword search:
  - match if `title LIKE %q% OR description LIKE %q%`
  - case-insensitive if possible (Db2 has collation options; for now use `LOWER(...) LIKE LOWER(...)` if supported by H2/Db2)

- Tag filter:
  - If tags provided, return tasks that have ALL tags (or ANY tags). Pick one and document.
  - Recommendation: ANY tags initially; ALL tags requires grouping/having logic.

- Deleted handling:
  - default: `deleted_at IS NULL`
  - if `deleted=true`: `deleted_at IS NOT NULL`

Pagination

- Return `total` (count query) + items.
- Ensure count query matches filters.

Acceptance criteria

- Code can express all required list filters/sorts.
- No SQL injection risk.
- H2 test mode can run the queries.

Notes

- If implementing "ALL tags" is low-risk for you, do it; otherwise implement "ANY tags" and note it in docs.
