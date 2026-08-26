# Smart Fitness API

P0 modular monolith: Spring Boot 3.3 / Java 21 / PostgreSQL 16 / Redis / MyBatis-Plus / Flyway.

## Quick start

```bash
cp .env.example .env
docker compose -f docker/docker-compose.yml up -d
mvn -pl smart-fitness-api -am package
mvn -pl smart-fitness-api spring-boot:run
```

Health: `GET http://localhost:8080/v1/health`

OpenAPI: `http://localhost:8080/swagger-ui.html`

Set `LLM_PROVIDER=fake` (default) to run without an external model. Usage rows are still written.
Set `LLM_PROVIDER=openai` plus `LLM_API_KEY` / `LLM_BASE_URL` / `LLM_MODEL` for D7.
