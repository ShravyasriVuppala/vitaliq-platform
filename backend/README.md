# VitalIQ Backend

Spring Boot 4 / Java 21 REST API — the core server for the VitalIQ fitness & nutrition platform.

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Setup & Installation](#setup--installation)
3. [Technology Choices](#technology-choices)
4. [Configuration Reference](#configuration-reference)
5. [Authentication](#authentication)
6. [API Reference](#api-reference)
7. [Database Schema](#database-schema)
8. [AI Integration](#ai-integration)
9. [Kafka & Elasticsearch Pipeline](#kafka--elasticsearch-pipeline)
10. [Development Guide](#development-guide)
11. [Troubleshooting](#troubleshooting)

---

## Project Structure

```
src/main/java/com/vitaliq/vitaliq_platform/
│
├── controller/
│   ├── AuthController.java              # /auth — register, login, refresh, logout
│   ├── ApiKeyController.java            # /api/auth/api-keys — generate, revoke
│   ├── WorkoutController.java           # /api/workouts — log, history, from-template
│   ├── WorkoutTemplateController.java   # /api/templates — CRUD, fork
│   ├── ExerciseController.java          # /api/exercises — search, create
│   ├── NutritionController.java         # /api/nutrition — generate plan, history
│   ├── DashboardController.java         # /api/dashboard — summary, volume, muscle groups
│   └── UserProfileController.java       # /api/profile, /api/body-metrics, /api/goals
│
├── service/
│   ├── AuthService.java                 # Registration, JWT issuance, token refresh
│   ├── ApiKeyService.java               # Key generation, revocation, scope validation
│   ├── WorkoutService.java              # Log workouts, retrieve history
│   ├── WorkoutFromTemplateService.java  # Log from template with set modifications
│   ├── WorkoutTemplateService.java      # Template CRUD + fork
│   ├── ExerciseService.java             # Exercise search + user exercise creation
│   ├── NutritionService.java            # AI nutrition plan generation
│   ├── DashboardService.java            # Elasticsearch aggregation queries
│   ├── UserProfileService.java          # Profile, body metrics, goals
│   ├── AiChatService.java               # Provider interface
│   ├── ClaudeAiChatService.java         # Anthropic SDK implementation
│   └── OpenAiChatService.java           # OpenAI SDK implementation
│
├── model/
│   ├── auth/
│   │   ├── User.java                    # users table — UUID PK, email, passwordHash
│   │   ├── RefreshToken.java            # refresh_tokens — hashed token + expiry
│   │   └── ApiKey.java                  # api_keys — prefixed key, comma-sep scopes
│   ├── workout/
│   │   ├── Workout.java                 # workouts — name, times, totalVolumeLbs
│   │   ├── WorkoutTemplate.java         # workout_templates — type, forkedFromId
│   │   ├── WorkoutExercise.java         # workout_exercises — ordered FK to Exercise
│   │   ├── TemplateExercise.java        # template_exercises — plannedSets, orderIndex
│   │   ├── ExerciseSet.java             # exercise_sets — abstract, JPA JOINED inheritance
│   │   ├── WeightedSet.java             # dtype=WEIGHTED — reps, weightLbs, equipmentType
│   │   └── CardioSet.java               # dtype=CARDIO — durationMinutes, distanceMiles
│   ├── user/
│   │   ├── UserProfile.java             # user_profiles — height, DOB, lifestyle
│   │   ├── UserGoal.java                # user_goals — goalType, targetDate
│   │   └── BodyMetricsLog.java          # body_metrics_log — weight, bodyFat%, date
│   └── nutrition/
│       ├── NutritionPlan.java           # nutrition_plans — macros, aiPrompt, aiResponse
│       ├── Meal.java                    # meals — mealType, time
│       └── MealItem.java                # meal_items — food, portion, calories
│
├── security/
│   ├── SecurityConfig.java              # Filter chain — ApiKey → JWT → default
│   ├── JwtFilter.java                   # Validates Bearer tokens per request
│   ├── ApiKeyAuthenticationFilter.java  # Validates X-API-Key header + scopes
│   ├── JwtUtil.java                     # Token generation and validation (JJWT 0.13)
│   ├── VitalIqUserDetails.java          # UserDetails carrying nullable ApiKey
│   └── UserDetailsServiceImpl.java      # Loads user by email
│
├── kafka/
│   ├── WorkoutKafkaProducer.java        # Publishes WorkoutEvent on workout log
│   └── WorkoutKafkaConsumer.java        # Consumes event → indexes WorkoutDocument
│
├── document/
│   └── WorkoutDocument.java             # @Document for Elasticsearch
│
├── scheduler/
│   └── WorkoutIndexingRecoveryJob.java  # Retries workouts where isIndexed=false
│
└── exception/
    └── GlobalExceptionHandler.java      # @RestControllerAdvice — maps exceptions to HTTP
```

---

## Setup & Installation

### Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 21+ |
| Maven | 3.9+ |
| Docker + Docker Compose | any recent |
| Node.js (MCP server only) | 20+ |

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 17** on `:5432` (db: `vitaliqdb`, user: `vitaliq`, password: `vitaliq123`)
- **Apache Kafka** (KRaft mode, no ZooKeeper) on `:9092`
- **Elasticsearch 9.x** on `:9200`
- **Kibana** on `:5601`

### 2. Run the Application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile uses `application-local.yml` which pre-fills all environment variables with Docker Compose defaults — no `.env` file needed for local development.

The server starts at `http://localhost:8080`. Hibernate auto-creates all tables on first boot (`spring.jpa.hibernate.ddl-auto: update`).

### 3. Verify

```bash
# Register a user
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"athlete@example.com","password":"strongpass"}' | jq .

# Login
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"athlete@example.com","password":"strongpass"}' | jq .
```

---

## Technology Choices

| Component | Choice | Why |
|-----------|--------|-----|
| **Framework** | Spring Boot 4 | Enterprise-grade, battle-tested, excellent ecosystem |
| **Database** | PostgreSQL | ACID compliance, JSONB for flexibility, proven at scale |
| **Messaging** | Kafka (KRaft) | Durable event log, horizontal scaling, fault tolerance |
| **Search** | Elasticsearch | Sub-second aggregations for dashboards, full-text search |
| **AI** | Anthropic + OpenAI | Pluggable design, not locked into one provider |
| **MCP** | Node.js stdio | Fast startup (~100ms), native Claude Desktop support |
## Configuration Reference

Configuration is fully environment-variable driven. The `local` profile provides safe defaults for development.

### `application.yml` (production template)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: vitaliq-group
      auto-offset-reset: earliest
    producer:
      acks: "0"

  elasticsearch:
    uris: ${ELASTICSEARCH_URIS}

ai:
  provider: ${AI_PROVIDER}               # "claude" or "openai"
  openai:
    model: ${OPENAI_MODEL}               # e.g. gpt-4o-mini
    max-tokens: ${OPENAI_MAX_TOKENS}
  anthropic:
    model: ${ANTHROPIC_MODEL}            # e.g. claude-haiku-4-5-20251001
    max-tokens: ${ANTHROPIC_MAX_TOKENS}

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION}   # ms
  refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION} # ms
```

### Environment Variables (production)

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://db:5432/vitaliqdb` |
| `DB_USERNAME` | DB user | `vitaliq` |
| `DB_PASSWORD` | DB password | — |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker(s) | `kafka:9092` |
| `ELASTICSEARCH_URIS` | ES node(s) | `http://es:9200` |
| `AI_PROVIDER` | `claude` or `openai` | `claude` |
| `ANTHROPIC_MODEL` | Claude model ID | `claude-haiku-4-5-20251001` |
| `ANTHROPIC_MAX_TOKENS` | Max response tokens | `2048` |
| `OPENAI_MODEL` | OpenAI model | `gpt-4o-mini` |
| `OPENAI_MAX_TOKENS` | Max response tokens | `2048` |
| `JWT_SECRET` | 256-bit hex secret | `your-256-bit-hex-secret` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL (ms) | `900000` |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL (ms) | `604800000` |

---

## Authentication

VitalIQ supports two authentication modes that coexist on the same security filter chain.

### 1. JWT (for human users / web clients)

```
POST /auth/register   →  { email, password }      →  { accessToken, refreshToken }
POST /auth/login      →  { email, password }      →  { accessToken, refreshToken }
POST /auth/refresh    →  { refreshToken }          →  { accessToken, refreshToken }
POST /auth/logout     →  { refreshToken }          →  "Logged out successfully"
```

Use the `accessToken` as a `Bearer` token:
```
Authorization: Bearer eyJhbGc...
```

Access tokens expire in **15 minutes**. Refresh tokens expire in **7 days** and are stored hashed in the database.

### 2. API Keys (for MCP server / programmatic access)

API keys enable scoped, long-lived access without JWT rotation. Each key carries a comma-separated scope string.

**Generate a key** (requires valid JWT):
```bash
curl -X POST http://localhost:8080/api/auth/api-keys/generate \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"name": "Claude MCP Server", "scope": "workouts,nutrition"}'
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Claude MCP Server",
  "key": "vk_live_abc123...",
  "scope": "workouts,nutrition",
  "createdAt": "2025-04-30T12:00:00"
}
```

Use the key on subsequent calls:
```
X-API-Key: vk_live_abc123...
```

**Available scopes:**

| Scope | Grants access to |
|-------|-----------------|
| `workouts` | Log workouts, templates, exercises, dashboard |
| `nutrition` | Generate and retrieve nutrition plans |

**Revoke a key:**
```bash
curl -X DELETE http://localhost:8080/api/auth/api-keys/{keyId} \
  -H "Authorization: Bearer $JWT"
```

### Security Filter Order

```
Request
  └─► ApiKeyAuthenticationFilter   (checks X-API-Key, sets principal)
        └─► JwtFilter               (checks Bearer token, sets principal)
              └─► UsernamePasswordAuthenticationFilter (Spring default)
```

If a request carries both headers, the API key filter wins. If neither header is present, the request hits a protected endpoint as unauthenticated and returns `401`.

---

## API Reference

All protected endpoints require either `Authorization: Bearer <token>` or `X-API-Key: <key>`.

### Auth  `/auth`

| Method | Path | Body | Response |
|--------|------|------|----------|
| `POST` | `/auth/register` | `{email, password}` | `{accessToken, refreshToken}` |
| `POST` | `/auth/login` | `{email, password}` | `{accessToken, refreshToken}` |
| `POST` | `/auth/refresh` | `{refreshToken}` | `{accessToken, refreshToken}` |
| `POST` | `/auth/logout` | `{refreshToken}` | `200 OK` |

### API Keys  `/api/auth/api-keys`

| Method | Path | Body | Response |
|--------|------|------|----------|
| `POST` | `/api/auth/api-keys/generate` | `{name, scope}` | `ApiKeyResponse` `201` |
| `DELETE` | `/api/auth/api-keys/{keyId}` | — | `204 No Content` |

### Workouts  `/api/workouts`

| Method | Path | Body / Params | Response |
|--------|------|--------------|----------|
| `POST` | `/api/workouts` | `LogWorkoutRequest` | `WorkoutResponse` `201` |
| `POST` | `/api/workouts/from-template` | `LogWorkoutFromTemplateRequest` | `WorkoutResponse` `201` |
| `GET` | `/api/workouts` | — | `List<WorkoutResponse>` |
| `GET` | `/api/workouts/{id}` | path: UUID | `WorkoutResponse` |

**Log a workout — example:**
```json
POST /api/workouts
{
  "name": "Push Day",
  "startTime": "2025-04-30T09:00:00",
  "endTime": "2025-04-30T10:15:00",
  "exercises": [
    {
      "exerciseId": "uuid-of-bench-press",
      "sets": [
        { "type": "WEIGHTED", "reps": 10, "weightLbs": 185, "equipmentType": "BARBELL" },
        { "type": "WEIGHTED", "reps": 8,  "weightLbs": 195, "equipmentType": "BARBELL" }
      ]
    },
    {
      "exerciseId": "uuid-of-treadmill",
      "sets": [
        { "type": "CARDIO", "durationMinutes": 20, "distanceMiles": 2.0 }
      ]
    }
  ]
}
```

**Log from template — example:**
```json
POST /api/workouts/from-template
{
  "templateId": "uuid-of-push-day-template",
  "updateTemplate": false,
  "exerciseModifications": [
    {
      "exerciseId": "uuid-of-bench-press",
      "modifiedSets": [
        { "type": "WEIGHTED", "reps": 12, "weightLbs": 200, "equipmentType": "BARBELL" }
      ]
    }
  ]
}
```

### Templates  `/api/templates`

| Method | Path | Body | Response |
|--------|------|------|----------|
| `GET` | `/api/templates` | — | `List<TemplateResponse>` |
| `POST` | `/api/templates` | `CreateTemplateRequest` | `TemplateResponse` `201` |
| `GET` | `/api/templates/{id}` | — | `TemplateResponse` |
| `PUT` | `/api/templates/{id}` | `CreateTemplateRequest` | `TemplateResponse` |
| `DELETE` | `/api/templates/{id}` | — | `204 No Content` |
| `POST` | `/api/templates/{id}/fork` | — | `TemplateResponse` `201` |

### Exercises  `/api/exercises`

| Method | Path | Body / Params | Response |
|--------|------|--------------|----------|
| `GET` | `/api/exercises` | `?category=WEIGHTED&muscleGroup=CHEST` | `List<ExerciseResponse>` |
| `POST` | `/api/exercises` | `CreateExerciseRequest` | `ExerciseResponse` |

**Exercise categories:** `WEIGHTED`, `CARDIO`

**Muscle groups:** `CHEST`, `BACK`, `LEGS`, `SHOULDERS`, `BICEPS`, `TRICEPS`, `FOREARMS`, `CORE`, `GLUTES`, `CARDIO`

**Equipment types:** `DUMBBELL`, `BARBELL`, `MACHINE`, `KETTLEBELL`, `CABLE`, `BODYWEIGHT`

### Nutrition  `/api/nutrition`

| Method | Path | Body | Response |
|--------|------|------|----------|
| `POST` | `/api/nutrition/generate` | `{planDate?}` | `NutritionPlanResponse` `201` |
| `GET` | `/api/nutrition/active` | — | `NutritionPlanResponse` |
| `GET` | `/api/nutrition/history` | — | `List<NutritionPlanResponse>` |

### Dashboard  `/api/dashboard`

| Method | Path | Response |
|--------|------|----------|
| `GET` | `/api/dashboard/summary` | `DashboardSummaryResponse` |
| `GET` | `/api/dashboard/volume` | `List<VolumeDataPoint>` |
| `GET` | `/api/dashboard/muscle-groups` | `List<MuscleGroupCount>` |

Dashboard queries run against **Elasticsearch aggregations** — not PostgreSQL — for fast analytics.

### User Profile  `/api`

| Method | Path | Body | Response |
|--------|------|------|----------|
| `POST` | `/api/profile` | `CreateProfileRequest` | `ProfileResponse` |
| `GET` | `/api/profile` | — | `ProfileResponse` |
| `PUT` | `/api/profile` | `UpdateProfileRequest` | `ProfileResponse` |
| `POST` | `/api/body-metrics` | `LogBodyMetricsRequest` | `BodyMetricsResponse` |
| `GET` | `/api/body-metrics` | — | `List<BodyMetricsResponse>` |
| `POST` | `/api/goals` | `SetGoalRequest` | `GoalResponse` |
| `GET` | `/api/goals/active` | — | `GoalResponse` |

---

## Database Schema

### Core Tables

```
users
  id UUID PK
  email VARCHAR UNIQUE NOT NULL
  password_hash VARCHAR NOT NULL
  is_active BOOLEAN DEFAULT true
  created_at, updated_at TIMESTAMP

api_keys
  id UUID PK
  user_id UUID FK → users
  name VARCHAR
  key_hash VARCHAR UNIQUE         ← stored hashed; full key shown once
  scope VARCHAR                   ← e.g. "workouts,nutrition"
  created_at TIMESTAMP

workouts
  id UUID PK
  user_id UUID FK → users
  name VARCHAR NOT NULL
  notes TEXT
  start_time, end_time TIMESTAMP
  total_volume_lbs DOUBLE
  template_id UUID (nullable)     ← set when logged from a template
  is_indexed BOOLEAN DEFAULT false
  created_at, updated_at TIMESTAMP

workout_templates
  id UUID PK
  name VARCHAR NOT NULL
  description TEXT
  template_type VARCHAR            ← 'SYSTEM' | 'USER'
  forked_from_id UUID (nullable)
  created_by UUID FK → users (nullable for SYSTEM)
  created_at, updated_at TIMESTAMP

exercises
  id UUID PK
  name VARCHAR NOT NULL
  description TEXT
  category VARCHAR                 ← 'WEIGHTED' | 'CARDIO'
  primary_muscle_group VARCHAR
  equipment_type VARCHAR (nullable)
  is_system_exercise BOOLEAN NOT NULL
  created_by UUID FK → users (nullable for system exercises)

exercise_sets                      ← JOINED inheritance table
  id UUID PK
  workout_exercise_id UUID FK (nullable)
  template_exercise_id UUID FK (nullable)
  set_number INTEGER NOT NULL
  set_context VARCHAR              ← 'WORKOUT' | 'TEMPLATE'
  is_completed BOOLEAN
  dtype VARCHAR                    ← discriminator: 'WEIGHTED' | 'CARDIO'

weighted_sets (extends exercise_sets)
  id UUID PK FK → exercise_sets
  repetitions INTEGER
  weight_lbs DOUBLE
  equipment_type VARCHAR

cardio_sets (extends exercise_sets)
  id UUID PK FK → exercise_sets
  duration_minutes DOUBLE
  distance_miles DOUBLE

nutrition_plans
  id UUID PK
  user_id UUID FK → users
  user_goal_id UUID FK → user_goals
  plan_date DATE NOT NULL
  plan_name VARCHAR
  is_active BOOLEAN NOT NULL
  ai_prompt TEXT                   ← full prompt sent to AI
  ai_response TEXT                 ← raw AI response
  total_calories, total_protein_g, total_carbs_g, total_fats_g DOUBLE
  created_at TIMESTAMP
```

### Entity Inheritance

`ExerciseSet` uses JPA **`JOINED` table inheritance** — a shared `exercise_sets` table holds common columns (set number, context, FK pointers) and subtype-specific columns live in `weighted_sets` / `cardio_sets` tables. The `dtype` discriminator column routes Java polymorphism at runtime.

This allows `instanceof WeightedSet` pattern matching in service code while keeping the schema normalized.

---

## AI Integration

The AI provider is swappable at runtime via the `AI_PROVIDER` environment variable.

```
ai.provider=claude   →  ClaudeAiChatService (anthropic-java SDK)
ai.provider=openai   →  OpenAiChatService   (openai-java SDK)
```

Both implement the `AiChatService` interface:
```java
public interface AiChatService {
    String chat(String prompt);
}
```

`NutritionService` builds the prompt by assembling user profile data — age (derived from DOB), gender, height, weight (from latest body metric log), lifestyle, workout frequency, health conditions, allergies, dietary preferences, and the active fitness goal. The raw AI response is stored alongside the parsed `NutritionPlan` for debugging and auditability.

---

## Kafka & Elasticsearch Pipeline

### Flow

```
POST /api/workouts
      │
      ▼
WorkoutService.logWorkout()
      │
      ├─► PostgreSQL  (persist Workout + WorkoutExercises + ExerciseSets)
      │
      └─► WorkoutKafkaProducer.publishWorkoutEvent()
                │
                ▼
          Kafka topic: workout-events
                │
                ▼
          WorkoutKafkaConsumer.consume()
                │
                ├─► WorkoutDocument mapped + indexed into Elasticsearch
                └─► workout.isIndexed = true   (update PostgreSQL flag)
```

### Recovery

`WorkoutIndexingRecoveryJob` runs on a fixed schedule and queries for workouts where `is_indexed = false`. Any unindexed workouts are re-published to Kafka. This handles cases where Elasticsearch was temporarily unavailable.

### Dashboard Queries

`DashboardService` bypasses PostgreSQL entirely and issues Elasticsearch aggregation queries:
- **Summary:** total workouts, total volume, avg duration (last 7 and 30 days)
- **Volume over time:** daily total volume as a time-series `DateHistogramAggregation`
- **Muscle groups:** term aggregation on `primaryMuscleGroup` field

---

## Development Guide

### Build

```bash
# Compile and run tests
./mvnw clean verify

# Package as JAR
./mvnw clean package -DskipTests

# Run packaged JAR
java -jar target/vitaliq-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Running with a Specific AI Provider

```bash
# Use Claude (default for local profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Use OpenAI instead
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-Dai.provider=openai -Dopenai.api.key=sk-..."
```

### Adding a New Scope-Protected Endpoint

1. Add the scope check at the top of the service method:
   ```java
   apiKeyService.validateApiKeyScope("workouts");
   ```
2. The existing `ApiKeyService.validateApiKeyScope()` reads from `SecurityContextHolder` — it's a no-op for JWT-authenticated users, so it's safe to add unconditionally.

### Kafka Topic

The `workout-events` topic is auto-created by the Kafka broker (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=true` in Docker Compose). No manual topic creation needed locally.

---

## Troubleshooting

### Application won't start — `LazyInitializationException` in security filter

**Cause:** A `@ManyToOne(fetch = LAZY)` association was accessed after the JPA session closed inside a Spring Security filter (filters run outside transaction scope).

**Fix:** Change the affected field to `FetchType.EAGER`, or load the association within a `@Transactional` service call.  
The `ApiKey.user` field uses `FetchType.EAGER` for exactly this reason.

---

### `401 Unauthorized` on all requests after API key generation

**Check 1:** The key must be sent in the `X-API-Key` header, not `Authorization`.

**Check 2:** The key is stored **hashed** in the database. Copy the `key` field from the generation response immediately — it cannot be retrieved later.

---

### `403 Forbidden` — scope mismatch

```json
{ "error": "API key does not have required scope: workouts" }
```

Regenerate the key with the correct scopes:
```json
{ "name": "My Key", "scope": "workouts,nutrition" }
```

---

### Dashboard returns empty / zero stats

Elasticsearch may not have indexed any workouts yet. Check:

1. Kafka is running: `docker compose ps`
2. A workout exists in PostgreSQL with `is_indexed = true`
3. The Elasticsearch index exists:
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```

If `is_indexed = false` for recent workouts, the Kafka consumer may be lagging. Restart the application — the recovery scheduler will re-publish on next run.

---

### `Could not find acceptable representation` (406)

Add `Content-Type: application/json` to your request and `Accept: application/json` if needed.

---

### Hibernate creates duplicate columns on restart

`ddl-auto: update` is safe for additive changes but does not drop removed columns. For destructive schema changes during development, temporarily switch to `ddl-auto: create-drop` or manage migrations manually.

## Production Deployment

### Docker Multi-Stage Build

```dockerfile
FROM maven:3.9 AS builder
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=builder /app/target/vitaliq-platform-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Environment Setup (Docker Compose)

```yaml
services:
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      AI_PROVIDER: claude
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - db
      - kafka
      - elasticsearch
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```