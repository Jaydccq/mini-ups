# Repository Guidelines

## Project Structure & Modules
- `backend/`: Spring Boot (Java 17) service. Sources in `src/main/java`, tests in `src/test/java`, logs in `backend/logs`.
- `frontend/`: React + TypeScript (Vite). App code in `src/`, tests in `src/test` or `*.test.ts(x)`.
- `database/`: SQL schemas and seed data (`create_tables.sql`, `init.sql`).
- `proto/`: Protocol Buffers used by backend; compiled via Maven during build.
- `scripts/`: Deployment, CI helpers, and ops scripts.
- `docker-compose*.yml`, `start-all.sh`, `stop-all.sh`: Local stack via Docker.

## Build, Test, and Run
- Local dev (services on host): `./start-local.sh` (needs PostgreSQL on `5432` and Redis on `6380`).
- Full stack via Docker: `./start-all.sh` and `./stop-all.sh`.
- Backend:
  - Build: `cd backend && ./mvnw clean package`
  - Run: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
  - Tests: `./mvnw test` (unit), `./mvnw verify` (includes IT); coverage: `./mvnw clean test jacoco:report` → `target/site/jacoco/index.html`.
- Frontend:
  - Dev server: `cd frontend && npm run dev`
  - Build: `npm run build` | Preview: `npm run preview`
  - Tests: `npm test` | Coverage: `npm run test:coverage`

## Coding Style & Naming
- Java (backend): 4-space indent; packages `com.miniups...`; classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`. Prefer constructor injection; use Lombok where present.
- TypeScript/React (frontend): ESLint is configured (`frontend/.eslintrc.cjs`). Components `PascalCase.tsx`; hooks prefixed `use*` in `src/hooks`; keep directories lowercase. Run `npm run lint` and `npm run lint:fix`.

## Testing Guidelines
- Backend: JUnit 5 + Mockito. Name unit tests `*Test.java`; integration tests `*IT.java` or `*IntegrationTest.java` (picked up by Failsafe). Use `run-tests.sh` or `run-user-tests.sh` for common flows.
- Frontend: Vitest + Testing Library. Prefer tests colocated with components or under `src/test`. Aim for meaningful coverage of core flows.

## Commit & PR Guidelines
- Commits: History is mixed; please adopt Conventional Commits (e.g., `feat(auth): add JWT refresh`) for clarity.
- PRs: Include purpose, linked issues, how to test, and for UI changes attach screenshots. Keep diffs focused; update docs when behavior or APIs change.

## Security & Config
- Never commit secrets. Copy `.env.example` to configure local values. JWT, DB, and Redis are set in `start-local.sh`. Verify ports: backend `8081`, frontend `3001`.
