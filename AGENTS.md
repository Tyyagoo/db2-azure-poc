# AGENTS.md

This repo is a Quarkus (3.32.x) + Renarde application built with Maven Wrapper and targeting Java 25.
Primary goals for agentic changes:
- Follow TDD (tests first), keep the suite green.
- Write clean, idiomatic Java.
- Prefer real DB2-backed behavior (not static/in-memory state) when implementing persistence.

Repo conventions
- Renarde templates live under `src/main/resources/templates/<ControllerClass>/<method>.html`.
- Web Bundler assets live under `src/main/resources/web/`.
- CI runs `mvn verify` (`.github/workflows/ci.yml`).

------------------------------------------------------------------------

## TDD (Required)

Default workflow for any behavior change (feature, bug fix, refactor):
1) Write a failing test that describes the desired behavior (red).
2) Implement the smallest change to pass (green).
3) Refactor for readability/design (still green).
4) Add edge/negative cases as tests.

Test quality bar
- Tests describe behavior and observable outcomes, not internal implementation details.
- Keep tests deterministic (no sleeps, no time-dependent assertions unless you control time).
- Prefer small tests with clear Arrange/Act/Assert structure.

------------------------------------------------------------------------

## Commands (Windows-first)

Use Maven Wrapper on Windows:
- `\.\mvnw.cmd <goal>`

Build + tests
- Run unit tests: `\.\mvnw.cmd test`
- CI-equivalent (recommended before handoff): `\.\mvnw.cmd verify -B`

Run a single unit test (Surefire)
- Single test class: `\.\mvnw.cmd -Dtest=MyTest test`
- Single test method: `\.\mvnw.cmd -Dtest=MyTest#myMethod test`
- Pattern: `\.\mvnw.cmd -Dtest=*Todo* test`

Integration tests (Failsafe)
- Note: ITs are skipped by default (`skipITs=true` in `pom.xml`).
- Run ITs: `\.\mvnw.cmd -DskipITs=false verify`
- Single IT class: `\.\mvnw.cmd -DskipITs=false -Dit.test=MyIT verify`
- Single IT method: `\.\mvnw.cmd -DskipITs=false -Dit.test=MyIT#myMethod verify`

Do not start the dev server unless explicitly needed; prefer tests as the feedback loop.

------------------------------------------------------------------------

## Java Style

Formatting
- Keep methods small; extract helpers early; avoid deep nesting.

Imports
- No wildcard imports.
- Group imports: `java.*`, `jakarta.*`, `org.*`, `io.*`, then project packages.

Types
- Prefer `java.time` over `java.util.Date` in new code.
- Avoid public mutable fields for new/changed code; prefer encapsulation.
- Use generics; avoid raw types.

Error handling
- Do not swallow exceptions.
- Add context when rethrowing; do not leak secrets in messages.
- Validate inputs with Bean Validation (`jakarta.validation`) where appropriate.

Logging
- Keep logs actionable and context-rich.
- Never log secrets (passwords, tokens, DB URLs with credentials).

------------------------------------------------------------------------

## Renarde + DB2 Expectations

Renarde
- Keep controllers thin; move business logic into services.
- Keep templates mostly presentation; move complex logic into Java or Qute extensions.
- Use `{uri:...}` for links and follow the template naming conventions.

DB2
- Prefer implementing persistence via Quarkus datasource + a proper data access layer.
- Do not expand static mutable in-memory state for real features (the current `model.Todo` list is demo code).
- Do not commit credentials; use environment/local config for secrets.

------------------------------------------------------------------------

## Definition of Done

- Tests written first (or updated first for behavior changes).
- The smallest relevant test command was run (single-test when possible).
- The suite is green (at least `\.\mvnw.cmd test`; `\.\mvnw.cmd verify -B` for non-trivial changes).
- Changes are minimal, readable, and consistent with repo conventions.
