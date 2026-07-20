# BudgetBrain API

A Spring Boot REST API that orchestrates **BudgetBrain**, an AI-powered personal finance tracker. It sits between the React frontend and the Python ML classifier, adding database persistence, AI-generated spending insights, and savings goal tracking.

<p>
  <img src="https://img.shields.io/badge/java-21-orange" alt="Java 21">
  <img src="https://img.shields.io/badge/framework-Spring%20Boot%203.x-6DB33F" alt="Spring Boot">
  <img src="https://img.shields.io/badge/build-Maven-red" alt="Maven">
  <img src="https://img.shields.io/badge/license-MIT-lightgrey" alt="MIT License">
</p>

**Live deployment:** https://budgetbrain-api.onrender.com

**Part of the BudgetBrain project:**
| Service | Description |
|---|---|
| [budgetbrain-frontend](https://github.com/Samvar-Jain/BudgetBrain-frontend) | React dashboard |
| **budgetbrain-api** *(this repo)* | Spring Boot backend orchestrator |
| [budgetbrain-ml](https://github.com/Samvar-Jain/BudgetBrain-ml) | Python ML classifier |

---

## How it works

```
  React Frontend (Vercel)
          │
          ▼
  Spring Boot API (Render)  ← this repo
          │
   ┌──────┼──────────────┐
   ▼      ▼              ▼
 Python  PostgreSQL    Gemini API
 ML svc  (transactions  (spending
 (classify) & goals)     insights)
```

It accepts CSV uploads from the frontend, forwards them to the Python ML service for classification, persists the results, generates plain-English spending insights with Gemini, and manages savings goals via a standard CRUD API.

## Quick Start

Requires **Java 21+**, **Maven 3.9+**, a **Gemini API key**, and the [Python ML service](https://github.com/Samvar-Jain/BudgetBrain-ml) running on `http://localhost:8000`.

```bash
git clone https://github.com/Samvar-Jain/BudgetBrain-api.git
cd budgetbrain-api

export GEMINI_API_KEY="your_actual_key_here"   # Windows: set GEMINI_API_KEY=...

./mvnw clean install
./mvnw spring-boot:run     # uses the 'dev' profile by default
```

Server runs at `http://localhost:8080`, backed by an in-memory H2 database — no setup required. Inspect it visually at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:budgetbrain`, user `sa`, blank password).

## Try It

```bash
curl -X POST http://localhost:8080/upload -F "file=@bank_statement.csv"
```

```json
[
  {
    "id": 1,
    "date": "2026-03-01",
    "description": "SWIGGY ORDER 88213",
    "amount": -420.0,
    "category": "Food",
    "classificationMethod": "keyword",
    "confidence": 1.0
  }
]
```

Full endpoint reference below.

<details>
<summary><strong>API reference</strong></summary>

### Upload & Classify

**`POST /upload`** — upload a CSV, classify transactions via the Python ML service, persist to the database.

```bash
curl -X POST http://localhost:8080/upload -F "file=@bank_statement.csv"
```

Multipart form, file field named `file`, CSV only.

| Code | Meaning |
|---|---|
| `200` | Transactions classified and saved |
| `400` | File is not `.csv` |
| `422` | CSV missing required columns |
| `500` | Python ML service unreachable or failed |

### Insights

**`GET /insights`** — generate an AI spending summary from stored transactions.

```bash
curl http://localhost:8080/insights
```
```json
{ "insight": "Your highest-spending category this month was Finance at ₹26,500..." }
```

| Code | Meaning |
|---|---|
| `200` | Insight generated |
| `400` | No transactions found — upload a CSV first |
| `500` | Gemini API call failed |

### Goals

**`GET /goals`** — list all savings goals.

```bash
curl http://localhost:8080/goals
```

**`POST /goals`** — create a goal.

```bash
curl -X POST http://localhost:8080/goals \
  -H "Content-Type: application/json" \
  -d '{"name":"Vacation Fund","targetAmount":100000,"currentAmount":25000,"deadline":"2026-12-31"}'
```

**`PUT /goals/{id}`** — update a goal.

```bash
curl -X PUT http://localhost:8080/goals/1 \
  -H "Content-Type: application/json" \
  -d '{"currentAmount": 15000}'
```

**`DELETE /goals/{id}`** — remove a goal. Returns `204 No Content`.

```bash
curl -X DELETE http://localhost:8080/goals/1
```

</details>

---

## Features

- **CSV upload & classification** — forwards statements to the Python ML service and persists results
- **Transaction management** — store, query, and analyze categorized transactions
- **AI insights** — Gemini-powered plain-English spending summaries
- **Savings goals** — create, update, and track progress toward financial goals
- **Dev/prod profiles** — H2 for local development, PostgreSQL for production
- **CORS support** — allows the React frontend from Vercel or localhost
- **No hardcoded secrets** — all credentials injected via environment variables

## Tech Stack

Spring Boot 3.x · Java 21 · Maven 3.9+ · Spring Data JPA (Hibernate) · H2 (dev) / PostgreSQL (prod) · Spring WebClient · Docker (multi-stage build)

---

<details>
<summary><strong>Configuration & environment variables</strong></summary>

### Local development (`application-dev.properties`)
- Database: H2 in-memory
- ML service: `http://localhost:8000`
- Port: `8080`
- Used automatically by `./mvnw spring-boot:run`

### Production (`application-prod.properties`)
- Database: PostgreSQL (credentials via environment variables)
- ML service: deployed service URL (via environment variable)
- Port: assigned by Render via `$PORT`
- Activated with `SPRING_PROFILES_ACTIVE=prod`

### Production environment variables

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_HOST=dpg-xxxxx.render.internal
DATABASE_PORT=5432
DATABASE_NAME=budgetbrain_db_xxxxx
DATABASE_USERNAME=user
DATABASE_PASSWORD=password
ML_SERVICE_URL=https://budgetbrain-ml.onrender.com
GEMINI_API_KEY=AIza...
PORT=10000
```

### CORS

```java
@CrossOrigin(origins = {
  "http://localhost:5173",                      // Local React dev
  "https://budgetbrain-frontend.vercel.app"      // Production React
})
```

Update these origins if deploying the frontend elsewhere.

</details>

<details>
<summary><strong>Deployment (Render)</strong></summary>

**1. Provision PostgreSQL**
Render → **New +** → **PostgreSQL** → name it `budgetbrain-db`, pick the same region as the API service, free instance tier. Note the connection details.

**2. Deploy the web service**
Render → **New +** → **Web Service** → select `BudgetBrain-api` → Runtime: **Docker** (auto-detects the `Dockerfile`) → same region as the database → add all production environment variables → **Create Web Service**.

**3. Verify**

```bash
curl https://budgetbrain-api-xxxxx.onrender.com/goals
```

Should return `[]` if the database connected correctly.

**Dockerfile** — multi-stage build: Maven + JDK 21 compiles the code, then a JRE 21 + Alpine stage runs the jar. Final image is ~150–200MB instead of 500MB+.

</details>

<details>
<summary><strong>Database schema</strong></summary>

```sql
CREATE TABLE transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  date DATE NOT NULL,
  description VARCHAR(255),
  amount DOUBLE,
  category VARCHAR(50),
  classification_method VARCHAR(20),
  confidence DOUBLE
);

CREATE TABLE goal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255),
  target_amount DOUBLE,
  current_amount DOUBLE,
  deadline DATE
);
```

</details>

<details>
<summary><strong>Testing locally (end-to-end)</strong></summary>

```bash
# Terminal 1 — Python ML service
cd ../budgetbrain-ml
source venv/bin/activate
uvicorn main:app --reload --port 8000

# Terminal 2 — Spring Boot API
cd ../budgetbrain-api
./mvnw spring-boot:run

# Terminal 3 — exercise the endpoints
curl -X POST http://localhost:8080/upload -F "file=@real_test_data.csv"
curl http://localhost:8080/insights
curl -X POST http://localhost:8080/goals \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","targetAmount":1000,"currentAmount":100,"deadline":"2026-12-31"}'
curl http://localhost:8080/goals
```

All endpoints should respond `200`/`201` with valid JSON.

</details>

<details>
<summary><strong>Project structure</strong></summary>

```
budgetbrain-api/
├── src/
│   ├── main/
│   │   ├── java/com/budgetbrain/budgetbrain_api/
│   │   │   ├── BudgetbrainApiApplication.java
│   │   │   ├── config/
│   │   │   │   ├── WebClientConfig.java          # Python ML service client
│   │   │   │   └── GeminiWebClientConfig.java    # Gemini API client
│   │   │   ├── controller/
│   │   │   │   ├── UploadController.java
│   │   │   │   └── GoalController.java
│   │   │   ├── model/
│   │   │   │   ├── Transaction.java
│   │   │   │   └── Goal.java
│   │   │   ├── repository/
│   │   │   │   ├── TransactionRepository.java
│   │   │   │   └── GoalRepository.java
│   │   │   ├── service/
│   │   │   │   └── InsightsService.java
│   │   │   └── dto/
│   │   │       └── ClassifyResponse.java
│   │   └── resources/
│   │       ├── application.properties          # Profile selector
│   │       ├── application-dev.properties       # Local H2 config
│   │       └── application-prod.properties      # Production Postgres config
│   └── test/
│       └── java/com/budgetbrain/budgetbrain_api/
│           └── BudgetbrainApiApplicationTests.java
├── Dockerfile              # Multi-stage build
├── .dockerignore
├── pom.xml
└── README.md
```

</details>

<details>
<summary><strong>Design decisions</strong></summary>

**Spring profiles (dev vs. prod)** — `application-dev.properties` uses H2 and localhost services for frictionless local development; `application-prod.properties` uses PostgreSQL and deployed service URLs. Same source code, environment-specific behavior, no secrets in the codebase.

**Environment variables for secrets** — API keys and database credentials are never committed. Set locally via `export GEMINI_API_KEY=...`, injected automatically by Render in production.

**WebClient over RestTemplate** — RestTemplate is in maintenance mode; WebClient is the modern Spring HTTP client. Using `.block()` keeps the code synchronous and simple, avoiding a full reactive chain while staying future-proof.

**Gemini "thinking mode" disabled** — early Gemini calls spent 200–290+ tokens on invisible reasoning, truncating output. Setting `thinkingConfig.thinkingBudget=0` cut token usage by ~80% and sped up responses, since transaction summarization doesn't need multi-step reasoning.

**Manual goal progress (not auto-calculated)** — auto-linking transactions to goals would require nontrivial categorization logic (which category counts toward which goal?). Instead, users update `currentAmount` manually via `PUT /goals/{id}`. Auto-linking via transaction tags is a planned v2 enhancement.

</details>

<details>
<summary><strong>Known limitations</strong></summary>

| Limitation | Details |
|---|---|
| **`/upload` 502s in production** | Works fine locally, but times out on Render. The Python ML service can take 30–60s+ to wake from a cold start, and Render's proxy timeout (~30–60s) fires before it responds. Fix would require queues/caching or a paid tier — out of scope for now. Test locally, or keep the ML service warm with periodic pings. |
| **H2 data loss** | In-memory H2 wipes all data on restart — fine for local dev, unsuitable for production. Always use PostgreSQL (`application-prod.properties`) in production. |
| **Gemini rate limits** | Free tier allows limited requests per minute; high-frequency insight generation may hit limits. Cache insights or throttle generation. |
| **No multi-tenant support** | All transactions live in a single schema — fine for personal use, not for multi-user SaaS. |

</details>

<details>
<summary><strong>Debugging</strong></summary>

**Service won't start**
- `java -version` shows 21+
- `./mvnw -v` runs cleanly
- `echo $GEMINI_API_KEY` is set
- Port 8080 isn't already in use (`lsof -i :8080`)

**Upload returns 502**
This is the known Render cold-start limitation above — test locally instead.

**H2 console won't load**
- Confirm the service is running
- Go directly to `http://localhost:8080/h2-console`
- Check the browser network tab for CORS errors (shouldn't occur locally)

**Goals endpoint returns an error**
- Production: confirm PostgreSQL is provisioned and credentials are correct
- Local: confirm the H2 console can reach the database
- Check Spring Boot logs for JPA/Hibernate errors

</details>

---

## Performance Notes

| Operation | Typical latency |
|---|---|
| Upload | 2–5s (depends on CSV size and ML cold-start) |
| Insights | 3–8s (Gemini API call) |
| Goals CRUD | <100ms |
| First request after idle | 30–60s (Render free-tier cold start) |

## Roadmap

Authentication & user isolation · transaction search and filtering · budget alerts · recurring transaction detection · custom tags/categories · historical analytics · CSV/PDF export · batch imports

## Contributing

This is a portfolio project — contributions aren't actively solicited, but the codebase is open for learning and reference.

## License

MIT

## Questions?

See the [BudgetBrain frontend repo](https://github.com/Samvar-Jain/BudgetBrain-frontend) for the full system overview and architecture diagrams.
