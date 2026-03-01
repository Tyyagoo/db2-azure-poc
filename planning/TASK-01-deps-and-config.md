# TASK-01: Dependencies + Quarkus Configuration

Goal

Add the Quarkus dependencies and baseline configuration needed to build a REST + DB-backed Task CRUD API with Flyway migrations, validation, and JSON.

Scope

- Update Maven dependencies (`pom.xml`).
- Populate `src/main/resources/application.properties`.
- Add `%test` datasource so CI can run without Db2.

Do not implement any feature logic yet.

Implementation steps

1) Add dependencies in `pom.xml`

Add (or confirm) these Quarkus extensions:

- REST + JSON: `quarkus-resteasy-reactive-jackson`
- Validation: `quarkus-hibernate-validator`
- ORM: `quarkus-hibernate-orm-panache`
- Flyway: `quarkus-flyway`

Testing deps:

- `quarkus-rest-assured` (scope test)
- `quarkus-jdbc-h2` (scope test)

Notes

- This repo already has `quarkus-jdbc-db2`.
- Keep imports/deps minimal; we’re not adding auth.

2) Add runtime configuration in `src/main/resources/application.properties`

Set up two profiles:

Main (Db2 via env vars; no secrets in git):

- `quarkus.datasource.db-kind=db2`
- `quarkus.datasource.jdbc.url=${DB2_JDBC_URL}`
- `quarkus.datasource.username=${DB2_USERNAME}`
- `quarkus.datasource.password=${DB2_PASSWORD}`
- `quarkus.hibernate-orm.database.generation=none`
- `quarkus.flyway.migrate-at-start=true`
- `quarkus.flyway.locations=db/migration`

Test profile (H2, DB2 compatibility):

- `%test.quarkus.datasource.db-kind=h2`
- `%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:tasks;MODE=DB2;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1`
- `%test.quarkus.datasource.username=sa`
- `%test.quarkus.datasource.password=sa`
- `%test.quarkus.hibernate-orm.database.generation=none`
- `%test.quarkus.flyway.migrate-at-start=true`

Optional helpful settings:

- SQL logging in dev: `%dev.quarkus.hibernate-orm.log.sql=true`

3) Verify baseline build

- Run: `\.\mvnw.cmd test`

Acceptance criteria

- Build passes locally with `\.\mvnw.cmd test`.
- No credentials or concrete Db2 URLs are hardcoded.
- App has Flyway enabled and points to `src/main/resources/db/migration`.

Follow-ups

- TASK-02 will add actual Flyway scripts; until then Flyway may do nothing.
