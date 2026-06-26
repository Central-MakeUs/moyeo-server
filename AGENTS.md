# Codex Working Rules

## Role

- Codex is an implementation support tool.
- A human is the final decision-maker for product policy, domain policy, and technical decisions.

## Source of Truth

- Read `docs/00-project-setup.md` before starting work.
- During feature development, read the relevant feature policy document before implementation.
- Do not invent or implement policies that are not documented.

## Allowed Scope

- Initial server base
- Health check
- Swagger/OpenAPI
- Actuator
- Local profile
- H2 local datasource
- MySQL datasource configuration template
- GitHub Actions CI

## Forbidden Scope

Do not implement the following until a human explicitly requests it. These restrictions describe the current initial-setup phase and may be changed for later development.

- Appointment-room domain logic
- Voting domain logic
- Participant domain logic
- Result domain logic
- JPA Entity
- Repository
- Authentication/JWT
- Social login
- Over-designed common response formats
- Over-designed common exception handling
- Docker
- Nginx
- Blue/Green deployment
- Zero-downtime deployment
- Redis
- Kafka
- NoSQL
- WebSocket
- MCP
- Sub-agents
- Production secrets
- Destructive commands

## Verification

Code changes are complete only after both commands pass.

```bash
./gradlew test
./gradlew build
```

## Development Principles

- Make small changes.
- Treat only verifiable work as complete.
- Do not guess domain policy.
- Leave unclear requirements as a TODO or proposal instead of implementing them.
- Add dependencies only for a clear reason.
- Never commit secrets to the repository.
## Self Review Before Completion

Before reporting completion, Codex must review the change using the following checklist.

### Scope

- Did the change stay within the requested scope?
- Did it avoid implementing undocumented domain policy?
- Did it avoid adding unnecessary dependencies?
- Did it avoid modifying unrelated files?

### Code Quality

- Is the change small and verifiable?
- Is the logic understandable without excessive abstraction?
- Is there any obvious missing validation, transaction boundary, or side effect?
- Is there any over-engineering for the current MVP stage?

### Tests and Build

- Did `./gradlew test` pass?
- Did `./gradlew build` pass?
- If HTTP verification is relevant, was it checked?

### Documentation

- Were related docs updated only when actual code, setting, or policy changed?
- If docs were not updated, is that reasonable?
- Do not update unrelated docs.
- Do not describe future plans as already implemented.
- If JPA entities, table names, columns, indexes, or relationships changed, update `docs/01-dbdiagram.md`.
- Keep the DBML in `docs/01-dbdiagram.md` copy-paste ready for dbdiagram.io.

### Report Format

After each task, report only:

1. Changed files
2. Summary
3. Verification result
4. Docs updated or not updated
5. Uncertain points
6. Intentionally not implemented items
