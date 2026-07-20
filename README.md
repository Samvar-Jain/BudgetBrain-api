# BudgetBrain API

A Spring Boot REST API backend for BudgetBrain, an AI-powered personal finance tracker. This service orchestrates CSV transaction uploads, coordinates classification via a Python ML service, stores categorized transactions in a database, and generates AI-powered spending insights using Google's Gemini API.

## Architecture

BudgetBrain API is the middle tier in a three-service architecture:
- **Python ML Service** (`budgetbrain-ml`) — CSV parsing, transaction classification (keyword rules + ML fallback)
- **Spring Boot API** (this service) — orchestration, database persistence, insights generation
- **React Frontend** (`budgetbrain-frontend`) — user interface, charting, goals management

The API bridges the gap between the ML classifier and the frontend, adding persistence (H2 or PostgreSQL), insights generation (Gemini API), and goal tracking.

## Tech Stack

- **Framework:** Spring Boot 3.x
- **Language:** Java 21
- **Build:** Maven
- **ORM:** Spring Data JPA (Hibernate)
- **Databases:**
    - H2 (in-memory, local development)
    - PostgreSQL (production, Render managed)
- **HTTP Client:** Spring WebClient (reactive-style, blocking mode)
- **External APIs:**
    - Python ML Service (CSV classification)
    - Google Gemini API (spending insights)

## Local Setup

### Prerequisites
- Java 21+
- Maven 3.9+
- Python ML service running locally on `http://localhost:8000`
- Gemini API key (set as `GEMINI_API_KEY` environment variable)

### Running Locally

```bash
# Clone and navigate
git clone https://github.com/Samvar-Jain/BudgetBrain-api.git
cd budgetbrain-api

# Set up environment
export GEMINI_API_KEY="your_actual_key_here"

# Build and run
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080` using the `dev` profile (H2 in-memory database, localhost ML service).

**H2 Console (local only):** `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:budgetbrain`
- Username: `sa`
- Password: (blank)

## API Endpoints

### Upload & Classify

**`POST /upload`**
- Accepts a CSV file (multipart form)
- Forwards to Python ML service for classification
- Saves categorized transactions to database
- Returns array of saved transactions with generated IDs

```bash
curl -X POST http://localhost:8080/upload \
  -F "file=@bank_statement.csv"
```

Response:
```json
[
  {
    "id": 1,
    "date": "2026-03-01",
    "description": "SWIGGY ORDER",
    "amount": -420.0,
    "category": "Food",
    "classificationMethod": "keyword",
    "confidence": 1.0
  }
  // ... more transactions
]
```

### Insights

**`GET /insights`**
- Queries all stored transactions
- Aggregates spending by category
- Sends summary to Gemini API
- Returns plain-English spending insight

```bash
curl http://localhost:8080/insights
```

Response:
```json
{
  "insight": "Your highest-spending category this month was Finance at ₹26,500. On a practical note, keeping your recurring bills and transport costs relatively low leaves flexibility for travel and lifestyle spending."
}
```

### Goals (CRUD)

**`GET /goals`** — List all savings goals
```bash
curl http://localhost:8080/goals
```

**`POST /goals`** — Create a goal
```bash
curl -X POST http://localhost:8080/goals \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Emergency Fund",
    "targetAmount": 50000,
    "currentAmount": 12000,
    "deadline": "2026-12-31"
  }'
```

**`PUT /goals/{id}`** — Update goal progress
```bash
curl -X PUT http://localhost:8080/goals/1 \
  -H "Content-Type: application/json" \
  -d '{"currentAmount": 15000}'
```

**`DELETE /goals/{id}`** — Delete a goal
```bash
curl -X DELETE http://localhost:8080/goals/1
```

## Configuration

### Local Development (`application-dev.properties`)
- H2 in-memory database
- Python ML service on `localhost:8000`
- Gemini API key from environment variable

### Production (`application-prod.properties`)
- PostgreSQL (connection details via environment variables)
- External service URLs via environment variables
- Spring profile: `prod`

**Environment variables (production only):**
- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_HOST` — Postgres hostname
- `DATABASE_PORT` — Postgres port (usually 5432)
- `DATABASE_NAME` — Database name
- `DATABASE_USERNAME` — Postgres user
- `DATABASE_PASSWORD` — Postgres password
- `ML_SERVICE_URL` — Deployed Python service URL
- `GEMINI_API_KEY` — Google Gemini API key
- `PORT` — Port to run on (assigned by Render)

## Deployment

### To Render

1. **Provision PostgreSQL database:**
    - New → PostgreSQL
    - Same region as web service
    - Note connection details (host, port, database, username, password)

2. **Deploy web service:**
    - New → Web Service → select this repo
    - Runtime: Docker (auto-detects `Dockerfile`)
    - Region: same as database
    - Add environment variables (see Configuration section above)
    - Deploy

3. **Verify:**
```bash
   curl https://budgetbrain-api-xxxxx.onrender.com/goals
```
Should return `[]` (empty array) if database is connected.

### Cold Starts & Spin-Down

Render's free tier spins down services after 15 minutes of inactivity. First request after spin-down takes 30-60 seconds to wake up. This is normal and expected — not a bug.

## Database Schema

### Transaction
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

### Goal
```sql
CREATE TABLE goal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255),
  target_amount DOUBLE,
  current_amount DOUBLE,
  deadline DATE
);
```

## Design Decisions

### Spring Profiles (dev vs prod)
- Separate configuration files allow H2 locally (zero setup, fast) while using proper PostgreSQL in production
- Environment variables keep secrets out of codebase
- Same source code, different databases per environment

### Lazy ML Classifier Training
- Python's `MLCategoryClassifier` trains on first real request, not at startup
- Reduces boot time but means first `/classify` call may be slow
- Local testing validated this behavior

### Gemini Thinking Disabled
- Initial implementation spent 200-290+ tokens on invisible "thinking" tokens
- Disabled thinking mode (`thinkingConfig.thinkingBudget=0`) since transaction summarization doesn't need reasoning
- Reduced token consumption and response latency by ~80%

### Manual Goal Progress
- `currentAmount` is user-entered, not auto-calculated from transaction history
- Avoids complex logic deciding which transactions count toward which goals
- Future enhancement: auto-linking transactions to goals via tags or category rules

## Known Limitations

### Cross-Service Communication on Render
The Spring Boot → Python ML service call (`POST /upload → /classify`) works perfectly on localhost but times out (502 Bad Gateway) when both services are deployed to Render's free tier. This appears to be related to:
- Render free tier's request timeout settings
- WebClient configuration for cross-service calls over the public internet
- The Python service itself responds correctly to direct curl requests, confirming it's not a Python-side issue

**Workaround:** Test the full pipeline locally, or upgrade to a paid Render plan with higher timeout allowances.

### H2 Database Ephemeral Storage
In-memory H2 (`jdbc:h2:mem:...`) loses all data on restart. This is fine for development but unsuitable for any persistent data in production. Always use PostgreSQL or another persistent database for production deployments.

## Future Enhancements

- Transaction history analytics (trends, spending velocity)
- Recurring transaction detection and categorization
- Budget alerts when spending exceeds category thresholds
- Export transactions to CSV
- Multi-currency support
- Connection pooling for high-concurrency scenarios
- Rate limiting to protect against abuse

## Testing

Local end-to-end flow:
```bash
# 1. Ensure Python service is running
curl http://localhost:8000/health

# 2. Upload a CSV
curl -X POST http://localhost:8080/upload -F "file=@real_test_data.csv"

# 3. Get an insight
curl http://localhost:8080/insights

# 4. Create and manage goals
curl -X POST http://localhost:8080/goals \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","targetAmount":1000,"currentAmount":100}'
```

## Contributing

This is a portfolio project for an 8-week full-stack challenge. Contributions are not actively sought, but the codebase is open for learning/reference.

## License

MIT