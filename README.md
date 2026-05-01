# VitalIQ Platform

> **AI-powered fitness & nutrition backend** — a production-grade Spring Boot API built across 13 development phases, integrating dual AI providers, event-driven workout indexing, and a Claude Desktop MCP integration.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-KRaft-231F20?logo=apachekafka&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-9.x-005571?logo=elasticsearch&logoColor=white)
![Claude AI](https://img.shields.io/badge/Claude_AI-Anthropic-8A2BE2)
![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o-412991?logo=openai&logoColor=white)
![MCP](https://img.shields.io/badge/MCP-9_Tools-0A84FF)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Clients                                 │
│                                                                 │
│   Web / Mobile          Claude Desktop          External API    │
│   (Bearer JWT)       (stdio MCP · 9 tools)      (X-API-Key)     │
└────────┬───────────────────┬────────────────────────┬───────────┘
         │                   │                        │
         ▼                   ▼                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                Spring Boot REST API  :8080                      │
│                                                                 │
│  ┌──────────────────────┐      ┌──────────────────────────────┐ │
│  │   Security Layer     │      │       8 REST Controllers     │ │
│  │  JwtFilter           │      │   /auth   /api/workouts      │ │
│  │  ApiKeyAuthFilter    │      │   /api/templates  /api/dash..│ │
│  │  BCrypt + JJWT 0.13  │      │  /api/exercises  /api/nutri..│ │
│  └──────────────────────┘      └──────────────┬───────────────┘ │
│                                               │                 │
│  ┌──────────────────────┐      ┌──────────────▼───────────────┐ │
│  │     AI Services      │      │       Service Layer          │ │
│  │  ClaudeAiChatService │◄─────│  WorkoutService              │ │
│  │  OpenAiChatService   │      │  NutritionService            │ │
│  │  (pluggable via env) │      │  WorkoutTemplateService      │ │
│  └──────────────────────┘      │  DashboardService  (+10 more)│ │
│                                └──────────────┬───────────────┘ │
│                                               │                 │
│  ┌──────────────────────┐      ┌──────────────▼───────────────┐ │
│  │   Kafka Producer     │◄─────│     JPA / Repository         │ │
│  │  (WorkoutEvent)      │      │  20+ Spring Data repos       │ │
│  └──────────┬───────────┘      └──────────────────────────────┘ │
└─────────────┼───────────────────────────────────────────────────┘
              │
     ┌────────▼──────────────────────────────────────┐
     │            Infrastructure                     │
     │                                               │
     │  PostgreSQL 17   Apache Kafka (KRaft mode)    │
     │  (primary store) ──► Kafka Consumer           │
     │                       └──► Elasticsearch 9.x  │
     │                            (full-text search +│
     │                             dashboard aggs)   │
     └───────────────────────────────────────────────┘
```

---

## What's Inside

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Framework** | Spring Boot 4.0.3 + Java 21 | Core application server |
| **Database** | PostgreSQL 17 + Spring Data JPA | Persistent storage, JPA inheritance |
| **Messaging** | Apache Kafka (KRaft, no ZooKeeper) | Async workout event pipeline |
| **Search** | Elasticsearch 9.x + Spring Data ES | Workout history, dashboard aggregations |
| **AI — Anthropic** | anthropic-java 2.18.0 | Claude-powered nutrition plans |
| **AI — OpenAI** | openai-java 4.30.0 | GPT-4o nutrition fallback |
| **Auth** | JWT (JJWT 0.13) + API Keys | Dual authentication modes |
| **MCP** | Node.js MCP SDK (stdio) | Claude Desktop native integration |
| **Infra** | Docker Compose | Local PostgreSQL + Kafka + Elasticsearch + Kibana |

---

## Feature Highlights

### Authentication & Authorization
- **JWT** access tokens (15 min) + refresh tokens (7 days)
- **Scoped API keys** (`workouts`, `nutrition`) for programmatic access
- Dual-filter Spring Security chain — JWT and API key authenticate independently
- BCrypt password hashing

### Workout Tracking
- Log workouts with **weighted** (reps/weight/equipment) or **cardio** (duration/distance) sets
- Workout **templates** — create, fork, update with per-exercise modifications
- Log a workout from a template with optional set overrides that can optionally update the template in the same request
- JPA **table-per-hierarchy** inheritance for polymorphic `ExerciseSet` types

### Event-Driven Indexing
- Every logged workout publishes a `WorkoutEvent` to Kafka
- Consumer picks it up asynchronously, indexes `WorkoutDocument` into Elasticsearch
- **Recovery scheduler** retries any workouts that failed indexing
- Dashboard queries hit Elasticsearch aggregations — not PostgreSQL

### AI-Powered Nutrition
- Pluggable AI provider (`AI_PROVIDER=claude` or `openai`) via environment variable
- Builds a rich prompt from user profile: age, gender, height, lifestyle, allergies, dietary preferences, active goals
- Returns structured `NutritionPlan` with per-meal macros (calories, protein, carbs, fat)
- Stores prompt + raw AI response for auditability

### Claude Desktop (MCP) Integration
9 tools exposed over stdio transport:

| Tool | Endpoint |
|------|---------|
| `get_exercises` | `GET /api/exercises` |
| `log_workout` | `POST /api/workouts` |
| `search_workouts` | `GET /api/workouts` |
| `get_dashboard` | `GET /api/dashboard/summary` |
| `generate_nutrition_plan` | `POST /api/nutrition/generate` |
| `get_templates` | `GET /api/templates` |
| `log_workout_from_template` | `POST /api/workouts/from-template` |
| `create_exercise` | `POST /api/exercises` |
| `create_template` | `POST /api/templates` |

---

## Quick Start

**Prerequisites:** Java 21, Maven 3.9+, Docker, Node.js 20+

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Run the backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 3. (Optional) Start the MCP server
cd mcp-server && npm install && node src/index.js
```

The API is live at `http://localhost:8080`.

Register an account:
```bash
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"secret123"}' | jq .
```

---

## Project Layout

```
vitaliq-platform/
├── src/main/java/com/vitaliq/vitaliq_platform/
│   ├── controller/        # 8 REST controllers
│   ├── service/           # 13+ services (incl. AI, Kafka)
│   ├── repository/        # 20+ Spring Data repositories
│   ├── model/             # JPA entities
│   │   ├── auth/          # User, RefreshToken, ApiKey
│   │   ├── workout/       # Workout, Template, ExerciseSet hierarchy
│   │   ├── user/          # UserProfile, UserGoal, BodyMetricsLog
│   │   └── nutrition/     # NutritionPlan, Meal, MealItem
│   ├── security/          # JWT + API key filters, SecurityConfig
│   ├── kafka/             # WorkoutKafkaProducer, WorkoutKafkaConsumer
│   ├── document/          # WorkoutDocument (Elasticsearch)
│   └── scheduler/         # WorkoutIndexingRecoveryJob
├── mcp-server/            # Node.js Claude Desktop MCP server
│   └── src/index.js       # 9 MCP tools over stdio transport
├── docker-compose.yaml    # PostgreSQL 17 · Kafka · Elasticsearch · Kibana
└── pom.xml
```

---

## Documentation

- [Backend Setup & API Reference](backend/README.md)
- [MCP Server Setup & Tools](mcp-server/README.md)