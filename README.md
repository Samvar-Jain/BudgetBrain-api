# BudgetBrain API
 
A Spring Boot REST API orchestrating BudgetBrain, an AI-powered personal finance tracker. This service bridges the Python ML classifier and React frontend, adding database persistence (H2/PostgreSQL), transaction storage, AI-generated spending insights (Gemini), and savings goals tracking.
 
**Part of the BudgetBrain project:**
- [budgetbrain-ml](https://github.com/Samvar-Jain/BudgetBrain-ml) — Python ML classifier
- [budgetbrain-frontend](https://github.com/Samvar-Jain/BudgetBrain-frontend) — React dashboard
**Live deployment:** https://budgetbrain-api.onrender.com
 
---
 
## Architecture
 
BudgetBrain API is the middle tier in a three-service architecture:
 
```
React Frontend (Vercel)
    ↓
Spring Boot API (Render) ← You are here
    ├→ Python ML Service (Render) — CSV classification
    ├→ PostgreSQL (Render) — Transaction & goal storage
    └→ Gemini API (Google) — AI spending insights
```
 
**Responsibilities:**
- Accept CSV uploads from React
- Forward to Python ML service for classification
- Persist classified transactions to database
- Generate AI insights using Gemini API
- Manage savings goals (CRUD operations)
- Serve React frontend via CORS
## Features
 
- **CSV Upload & Classification:** Receives bank statements, forwards to Python ML service, persists results
- **Transaction Management:** Store, query, and analyze categorized transactions
- **AI Insights:** Gemini-powered plain-English spending summaries from transaction data
- **Savings Goals:** Create, update, and track progress toward financial goals
- **Multi-Profile Configuration:** H2 for local dev, PostgreSQL for production
- **Environment-Based Secrets:** No hardcoded credentials, all via environment variables
- **CORS Support:** Allows React frontend from Vercel or localhost
- **H2 Web Console:** Visual database inspection (local development)
## Tech Stack
 
- **Framework:** Spring Boot 3.x
- **Language:** Java 21
- **Build Tool:** Maven 3.9+
- **ORM:** Spring Data JPA (Hibernate)
- **Databases:**
  - H2 (in-memory, local development)
  - PostgreSQL (production, Render managed)
- **HTTP Client:** Spring WebClient (async/reactive, blocking mode)
- **External APIs:**
  - Python ML Service (`/classify` endpoint)
  - Google Gemini API (insights generation)
- **Containerization:** Docker (multi-stage build for production)
## Local Setup
 
### Prerequisites
- Java 21+
- Maven 3.9+
- Python ML service running on `http://localhost:8000`
- Gemini API key
### Installation
 
```bash
git clone https://github.com/Samvar-Jain/BudgetBrain-api.git
cd budgetbrain-api
 
# Set environment variable (Linux/Mac)
export GEMINI_API_KEY="your_actual_key_here"
 
# On Windows, use:
# set GEMINI_API_KEY=your_actual_key_here
 
# Build
./mvnw clean install
 
# Run (uses 'dev' profile by default)
./mvnw spring-boot:run
```
 
Server runs at `http://localhost:8080`
 
### H2 Console (Local Only)
 
Access database UI: `http://localhost:8080/h2-console`
 
- **JDBC URL:** `jdbc:h2:mem:budgetbrain`
- **Username:** `sa`
- **Password:** (blank)
---
 
## API Endpoints
 
All endpoints require the backend to be running. Responses include appropriate HTTP status codes and error messages.
 
### Upload & Classify
 
**`POST /upload`** — Upload CSV, classify transactions, save to database
 
```bash
curl -X POST http://localhost:8080/upload \
  -F "file=@bank_statement.csv"
```
 
**Request:**
- Multipart form with file field `file`
- Accepted: CSV files only
**Response (200):**
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
  },
  {
    "id": 2,
    "date": "2026-03-01",
    "description": "SALARY CREDIT ACME INDIA",
    "amount": 85000.0,
    "category": "Income",
    "classificationMethod": "keyword",
    "confidence": 1.0
  }
  // ... more transactions
]
```
 
**Error Responses:**
- `400` — File is not .csv
- `422` — CSV format invalid (missing required columns)
- `500` — Python ML service unreachable or failed
### Get Insights
 
**`GET /insights`** — Generate AI spending summary from stored transactions
 
```bash
curl http://localhost:8080/insights
```
 
**Response (200):**
```json
{
  "insight": "Your highest-spending category this month was Finance at ₹26,500. On a practical note, keeping your recurring bills and transport costs relatively low leaves flexibility for travel and lifestyle spending."
}
```
 
**Error Responses:**
- `400` — No transactions found (upload a CSV first)
- `500` — Gemini API call failed
### List Goals
 
**`GET /goals`** — Retrieve all savings goals
 
```bash
curl http://localhost:8080/goals
```
 
**Response:**
```json
[
  {
    "id": 1,
    "name": "Emergency Fund",
    "targetAmount": 50000.0,
    "currentAmount": 12000.0,
    "deadline": "2026-12-31"
  }
]
```
 
### Create Goal
 
**`POST /goals`** — Add a new savings goal
 
```bash
curl -X POST http://localhost:8080/goals \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Vacation Fund",
    "targetAmount": 100000,
    "currentAmount": 25000,
    "deadline": "2026-12-31"
  }'
```
 
**Response (200):** Returns created goal with generated `id`
 
### Update Goal
 
**`PUT /goals/{id}`** — Update goal progress or details
 
```bash
curl -X PUT http://localhost:8080/goals/1 \
  -H "Content-Type: application/json" \
  -d '{"currentAmount": 15000}'
```
 
### Delete Goal
 
**`DELETE /goals/{id}`** — Remove a savings goal
 
```bash
curl -X DELETE http://localhost:8080/goals/1
```
 
**Response (204):** No content (success)
 
---
 
## Configuration
 
### Local Development Profile (`application-dev.properties`)
 
- **Database:** H2 in-memory
- **ML Service:** `http://localhost:8000`
- **Port:** 8080
Automatically used when running locally via `./mvnw spring-boot:run`
 
### Production Profile (`application-prod.properties`)
 
- **Database:** PostgreSQL (credentials via environment variables)
- **ML Service:** Deployed service URL (via environment variable)
- **Port:** Assigned by Render (via `$PORT` environment variable)
Activated with `SPRING_PROFILES_ACTIVE=prod`
 
### Environment Variables (Production Only)
 
```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_HOST=dpg-xxxxx.render.internal
DATABASE_PORT=5432
DATABASE_NAME=budgetbrain_db_xxxxx
DATABASE_USERNAME=user
DATABASE_PASSWORD=password
ML_SERVICE_URL=https://budgetbrain-ml.onrender.com
GEMINI_API_KEY=AIza... (your actual key)
PORT=10000  # (assigned by Render)
```
 
---
 
## Deployment
 
### Prerequisites
- GitHub account with repo connected to Render
- PostgreSQL database provisioned on Render
- Gemini API key (rotated, if previously exposed)
### Step-by-Step Deployment
 
#### 1. Provision PostgreSQL Database
- Go to **Render.com** → **New +** → **PostgreSQL**
- **Name:** `budgetbrain-db`
- **Region:** Choose same region as API service
- **Instance Type:** Free
- Note the connection details (Host, Port, Database, Username, Password)
#### 2. Deploy Web Service
- **New +** → **Web Service**
- **GitHub Repo:** Select `BudgetBrain-api`
- **Runtime:** Docker (auto-detects `Dockerfile`)
- **Region:** Same as database (critical for internal networking)
- **Environment Variables:** Add all the production ones (see above)
- Click **Create Web Service**
Render automatically detects `Dockerfile`, builds the image, and deploys.
 
#### 3. Verify Deployment
```bash
curl https://budgetbrain-api-xxxxx.onrender.com/goals
```
 
Should return `[]` (empty array) if database is connected correctly.
 
### Dockerfile
 
Multi-stage build optimizes image size:
- **Stage 1:** Maven + JDK 21 (compiles code)
- **Stage 2:** JRE 21 + Alpine (runs the jar, much smaller)
Result: ~150-200MB image instead of 500MB+
 
---
 
## Database Schema
 
### Transaction Table
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
```
 
Stores all uploaded and classified transactions.
 
### Goal Table
```sql
CREATE TABLE goal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255),
  target_amount DOUBLE,
  current_amount DOUBLE,
  deadline DATE
);
```
 
Tracks user-defined savings goals.
 
---
 
## Design Decisions
 
### Spring Profiles (Dev vs. Prod)
**Problem:** Need H2 for frictionless local dev, but PostgreSQL for production persistence.
 
**Solution:** Separate config files per profile.
- `application-dev.properties` — H2, localhost services
- `application-prod.properties` — PostgreSQL, deployed service URLs
- Single source code, environment-specific behavior
**Benefits:** No code changes when switching environments, secrets never in codebase.
 
### Environment Variables for Secrets
**Problem:** API keys and database credentials shouldn't be in Git history.
 
**Solution:** All secrets injected at runtime via environment variables.
- Local: `export GEMINI_API_KEY=...` before starting
- Render: Set in dashboard, auto-injected into container
### WebClient (Not RestTemplate)
**Problem:** RestTemplate is in maintenance mode; modern Spring apps use WebClient.
 
**Solution:** WebClient with `.block()` for synchronous behavior.
- Blocking mode keeps code simple (no reactive chains needed)
- Modern API, future-proof
- Proper timeout configuration available
### Gemini Thinking Mode Disabled
**Problem:** Initial Gemini calls spent 200-290+ tokens on invisible "reasoning," causing output truncation.
 
**Solution:** Set `thinkingConfig.thinkingBudget=0` in generation config.
- Transaction summarization doesn't need multi-step reasoning
- Reduced tokens by ~80%, faster response times
### Manual Goal Progress (Not Auto-Calculated)
**Problem:** Auto-linking goals to transactions requires complex categorization logic (which category counts toward which goal?).
 
**Solution:** User manually updates `currentAmount` via PUT endpoint.
- Simple, no ambiguity
- Future v2 enhancement: auto-linking with transaction tags
---
 
## Known Limitations
 
### 1. Spring Boot ↔ Python 502 Timeout (Production Only)
The `/upload` endpoint works perfectly on localhost but times out (502 Bad Gateway) when deployed to Render.
 
**Root Cause:**
- Python service takes 30-60+ seconds to wake from cold start (Render free tier)
- Render's proxy has a built-in timeout (~30-60 seconds)
- By the time Python responds, the timeout has already fired
**Workaround:**
- Test locally (fully works)
- Keep service warm with periodic pings
- Upgrade to paid Render tier (higher timeouts)
**Why we didn't fix it:**
- Can't increase WebClient timeout infinitely
- Can't make Python faster on free tier
- Would require architecture redesign (queues, caching)
### 2. H2 In-Memory Data Loss
In-memory H2 (`jdbc:h2:mem:...`) wipes all data on restart.
- Fine for local dev (fresh state each time)
- Unsuitable for production
**Solution:** Always use PostgreSQL in production (`application-prod.properties`)
 
### 3. Gemini API Rate Limits
Free tier allows limited requests per minute. High-frequency insight generation may hit limits.
 
**Solution:** Cache insights or limit generation frequency
 
### 4. No Multi-Tenant Support
Database stores all users' transactions in one schema. Fine for personal use, not for multi-user SaaS.
 
---
 
## Testing Locally
 
### End-to-End Workflow
 
```bash
# Terminal 1: Start Python ML service
cd ../budgetbrain-ml
source venv/bin/activate
uvicorn main:app --reload --port 8000
 
# Terminal 2: Start Spring Boot API
cd ../budgetbrain-api
./mvnw spring-boot:run
 
# Terminal 3: Test endpoints
# Upload a CSV
curl -X POST http://localhost:8080/upload -F "file=@real_test_data.csv"
 
# Get insights
curl http://localhost:8080/insights
 
# Create a goal
curl -X POST http://localhost:8080/goals \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","targetAmount":1000,"currentAmount":100,"deadline":"2026-12-31"}'
 
# List goals
curl http://localhost:8080/goals
```
 
All endpoints should respond with `200` or `201` and return valid JSON.
 
---
 
## Project Structure
 
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
│   │       ├── application.properties         # Profile selector
│   │       ├── application-dev.properties     # Local H2 config
│   │       └── application-prod.properties    # Production Postgres config
│   └── test/
│       └── java/com/budgetbrain/budgetbrain_api/
│           └── BudgetbrainApiApplicationTests.java
├── Dockerfile                  # Multi-stage build
├── .dockerignore              # Reduce Docker context
├── pom.xml                    # Maven dependencies & build config
└── README.md
```
 
---
 
## CORS Configuration
 
React frontend (Vercel) can reach this API because of:
 
```java
@CrossOrigin(origins = {
  "http://localhost:5173",                      // Local React dev
  "https://budgetbrain-frontend.vercel.app"     // Production React
})
```
 
If deploying to a different React URL, update these origins.
 
---
 
## Performance Notes
 
- **Upload:** ~2-5 seconds (depends on CSV size and Python ML cold-start time)
- **Insights:** ~3-8 seconds (Gemini API call, network-dependent)
- **Goals CRUD:** <100ms (local database)
- **First request:** Render services take 30-60s to wake from cold start (free tier)
---
 
## Future Enhancements
 
- Authentication & authorization (user isolation)
- Transaction search and filtering
- Budget alerts
- Recurring transaction detection
- Transaction tags / custom categorization
- Historical analytics (trends, velocity)
- Export to CSV/PDF
- Batch transaction imports
---
 
## Contributing
 
This is a portfolio project. Contributions are not actively solicited, but the codebase is open for learning and reference.
 
## License
 
MIT
 
---
 
## Debugging
 
### Service won't start
Check:
- Java 21+ installed: `java -version`
- Maven working: `./mvnw -v`
- GEMINI_API_KEY set: `echo $GEMINI_API_KEY`
- Port 8080 not in use: `lsof -i :8080`
### Upload returns 502
This is the known limitation — Python service is timing out on Render. Test locally instead.
 
### H2 console won't load
- Confirm service is running (`http://localhost:8080/health` returns OK)
- Try accessing directly: `http://localhost:8080/h2-console`
- Check browser network tab for CORS errors (shouldn't be any locally)
### Goals endpoint returns error
- Confirm PostgreSQL is provisioned and credentials are correct (production)
- Confirm H2 console can access database (local)
- Check Spring Boot logs for JPA/Hibernate errors
---
 
## Questions?
 
See the main [BudgetBrain Frontend docs](https://github.com/Samvar-Jain/BudgetBrain-frontend) for the full system overview and architecture diagrams.
