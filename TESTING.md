# Testing the Freight Search MVP

This is a demo app with **synthetic data only** — no real shipments, carriers, or customer data.

Stack: PostgreSQL (source of truth) → Elasticsearch (search index) → Spring Boot API → React/Vite frontend.

## Prerequisites

- Docker (for PostgreSQL and Elasticsearch)
- Java 17+ and Maven (for the backend)
  - macOS: `brew install openjdk@17 maven`
- Node.js and npm (for the frontend)

If `java`/`mvn` aren't on your PATH after installing via Homebrew on macOS, run each backend command with:
```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=/opt/homebrew/opt/maven/bin:$JAVA_HOME/bin:$PATH
```

## 1. Start PostgreSQL and Elasticsearch

```
docker compose up -d
```

This maps PostgreSQL to **localhost:5433** (not 5432) to avoid clashing with any PostgreSQL
instance already running on your machine. Elasticsearch is on localhost:9200, with security
disabled for local-only use.

Check both are up:
```
curl http://localhost:9200
docker exec $(docker compose ps -q postgres) pg_isready -U freight
```

## 2. Seed synthetic data into PostgreSQL

From `backend/`:
```
export DB_URL=jdbc:postgresql://localhost:5433/freight
export DB_USER=freight
export DB_PASSWORD=freight
mvn exec:java -Dexec.mainClass=com.uberfreight.search.scripts.SeedData
```

Expected output: `Seeded 300 synthetic shipments into PostgreSQL.`

Re-running this script truncates and reloads the table, so it's safe to run again.

## 3. Reindex PostgreSQL into Elasticsearch

Still from `backend/`, with the same `DB_*` env vars set:
```
export ES_URL=http://localhost:9200
export ES_INDEX=shipments
mvn exec:java -Dexec.mainClass=com.uberfreight.search.scripts.ReindexRunner
```

Expected output: `Reindexed 300 shipments (0 failed).`

This creates the `shipments` index (with mapping) if it doesn't exist, then bulk-upserts every
row using the PostgreSQL `id` as the Elasticsearch document `_id`. Run it again — the doc count
stays at 300 (no duplicates), because each upsert overwrites the same `_id`.

Verify directly against Elasticsearch:
```
curl "http://localhost:9200/_cat/indices?v"
```

## 4. Run the API

From `backend/`, with `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ES_URL`, `ES_INDEX` still set:
```
mvn spring-boot:run
```

Or build and run the jar directly:
```
mvn -DskipTests package
java -jar target/freight-search-0.1.0.jar
```

The API listens on **http://localhost:8080**.

### Check health
```
curl http://localhost:8080/health
```
Expected: `{"status":"ok","postgres":true,"elasticsearch":true}`

### Check search
```
curl "http://localhost:8080/shipments/search?page=1&page_size=5"
curl "http://localhost:8080/shipments/search?q=Chicago"
curl "http://localhost:8080/shipments/search?origin=Dallas"
curl "http://localhost:8080/shipments/search?equipment_type=Reefer"
curl "http://localhost:8080/shipments/search?min_rate=3000&max_rate=5000"
```

Each response has the shape:
```json
{ "items": [ ... ], "total": 123, "page": 1, "page_size": 20 }
```

### Check input validation
```
curl -i "http://localhost:8080/shipments/search?page_size=500"   # expect 400
curl -i "http://localhost:8080/shipments/search?min_rate=5000&max_rate=100"  # expect 400
```

## 5. Run the frontend

From `frontend/`:
```
npm install
npm run dev
```

Open **http://localhost:5173**. You should see the search page with a "Synthetic demo data" badge,
a search box, origin/destination/equipment/rate filters, and a results table.

The API must be running on port 8080 for the page to load results (CORS is configured in the
backend to allow `http://localhost:5173` by default — override with the `FRONTEND_ORIGIN` env var
if you run the frontend on a different port).

### Manual checks to run in the browser
- Load the page with no filters — should show 300 total shipments, paginated 20 at a time.
- Type a city into the search box (e.g. "Chicago") and submit — results should narrow.
- Set an equipment type and a min/max rate — results should narrow further.
- Clear all filters and search again — should return to the full result set.
- Stop the backend and reload/search — the page should show a readable error state, not a crash.
- Search for a nonsense query (e.g. "zzzzz") — should show a "No shipments match" empty state.

## What was verified while building this

- `mvn compile` and `mvn -DskipTests package` succeed.
- `npm run build` (tsc + vite) succeeds with no type errors.
- Full pipeline run end-to-end: Docker Compose up → seed (300 rows) → reindex (300 docs, 0
  failed) → API started → `/health` returned `postgres: true, elasticsearch: true` →
  `/shipments/search` verified with no filters, `q`, `origin`, `equipment_type`, and
  `min_rate`/`max_rate`, plus a validation case (`page_size=500` → 400).
- Reindex re-run confirmed idempotent (document `_version` incremented, doc count stayed at 300).
- CORS headers confirmed present for requests from `http://localhost:5173`.

Not verified in this session: the React UI was not visually tested in an actual browser (no
browser automation tool was available). The TypeScript build is type-clean and the frontend's
fetch/response shapes were checked by hand against the live API, but run through the manual
checks above yourself before considering the UI done.

## Known limitations

- No automated tests yet (unit tests for query building, search validation, and reindex
  idempotency are a reasonable next step).
- No authentication — this is a public read-only demo API, not production software.
- Single-node Elasticsearch with security disabled — local demo only, not a deployment pattern.
