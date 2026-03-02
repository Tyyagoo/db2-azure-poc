# Task CRUD App (Db2 + Flyway) - Execution Plan

This folder breaks the implementation into small, test-driven tasks. The intended execution flow is:

1) Open this file and pick the first unfinished task.
2) Open the referenced `planning/TASK-XX.md` and follow it step-by-step.
3) Implement with TDD (red -> green -> refactor). Keep the suite green.
4) When a task is done, come back here and mark it completed.
5) Move on to the next task.

Notes/constraints for this plan

- No auth/authorization for now (all endpoints are “public” in the sense they do not require authentication). Ownership checks are not implemented.
- Use IBM Db2 in real deployments; tests run on H2 (`MODE=DB2`) so CI can run without Db2.
- Persistence must be real DB-backed (no in-memory static lists).
- Soft delete is required (`deleted_at`), with restore + optional hard delete.
- Audit/history of changes is required (write an audit row per change).
- Concurrency protection is required (optimistic locking via `version` and HTTP `ETag` + `If-Match`).
- Flyway versioned SQL migrations are the source of truth for schema.

Recommended local workflow

- Baseline: `git status` then `\.\mvnw.cmd test`
- During work: run the smallest test command possible (see AGENTS.md)
- Before handoff: `\.\mvnw.cmd verify -B`

Task Index (work top-to-bottom)

- [x] TASK-01: Dependencies + Quarkus configuration (Flyway/ORM/REST/JSON/Validation/Test DB)
  - Details: `planning/TASK-01-deps-and-config.md`
- [x] TASK-02: Flyway migrations (Db2 DDL + indexes)
  - Details: `planning/TASK-02-flyway-migrations.md`
- [x] TASK-03: Domain model (entities + enums + soft delete + version)
  - Details: `planning/TASK-03-domain-model.md`
- [x] TASK-04: DTOs + validation (create/update/patch/bulk + responses)
  - Details: `planning/TASK-04-dtos-and-validation.md`
- [x] TASK-05: Query model (list/search/filter/sort/pagination)
  - Details: `planning/TASK-05-querying.md`
- [x] TASK-06: Service layer (CRUD + bulk + tags + trash/restore + audit)
  - Details: `planning/TASK-06-service-layer.md`
- [x] TASK-07: REST API resource (endpoints + ETag/If-Match)
  - Details: `planning/TASK-07-rest-api.md`
- [ ] TASK-08: Error handling (400/404 + concurrency preconditions)
  - Details: `planning/TASK-08-error-handling.md`
- [ ] TASK-09: Tests (QuarkusTest + RestAssured + Flyway/H2)
  - Details: `planning/TASK-09-tests.md`
- [ ] TASK-10: Polish (docs, examples, optional rate limit)
  - Details: `planning/TASK-10-polish.md`

Definition of Done (per task)

- Adds/updates tests first (or alongside) to specify behavior.
- `\.\mvnw.cmd test` stays green.
- No credentials committed (Db2 URL/user/pass via env vars).
- Schema changes only via new Flyway migration scripts.
