# TASK-10: Polish (Docs, Examples, Optional Rate Limit)

Goal

Make the feature easy to run and understand, and optionally add small non-functional improvements.

Deliverables

1) Documentation

- Update `README.md` with:
  - required env vars for Db2 (`DB2_JDBC_URL`, `DB2_USERNAME`, `DB2_PASSWORD`)
  - example curl commands for endpoints
  - explanation of ETag/If-Match usage
  - note: auth not implemented

2) API examples

- Provide sample JSON for:
  - create
  - bulk create
  - patch with add/remove tags
  - list filters

3) Optional: basic rate limiting

If desired, implement simple write rate limits using SmallRye Fault Tolerance:

- Add dependency `quarkus-smallrye-fault-tolerance`
- Annotate write endpoints with `@RateLimit` (choose a sensible default)

Keep it optional; do not block the core CRUD deliverable.

4) Optional: hardening

- Ensure tag normalization is consistent (lowercase, trim)
- Ensure bulk operations have clear error semantics (all-or-nothing)

Acceptance criteria

- README contains enough to run locally.
- Examples match the actual implemented API.
