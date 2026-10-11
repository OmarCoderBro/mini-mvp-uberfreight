# Freight Search MVP

Synthetic shipment search: PostgreSQL → Elasticsearch → Spring Boot API → React page.

## Run locally

Requires Docker, Java 17 + Maven (`brew install openjdk@17 maven`), and Node.js.

If `java` isn't found:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

**1. Start the databases** (from the repo root)
```bash
docker compose up -d
```

**2. Seed, index, and start the API** (one terminal, from `backend/`)
```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5433/freight DB_USER=freight DB_PASSWORD=freight
export ES_URL=http://localhost:9200 ES_INDEX=shipments

mvn exec:java -Dexec.mainClass=com.uberfreight.search.scripts.SeedData
mvn exec:java -Dexec.mainClass=com.uberfreight.search.scripts.ReindexRunner
mvn spring-boot:run
```

**3. Start the frontend** (second terminal, from `frontend/`)
```bash
cd frontend
npm install
npm run dev
```

Open the URL Vite prints (normally http://localhost:5173). Check the API at http://localhost:8080/health.

**Stop:** Ctrl+C both terminals, then `docker compose down`.

## Troubleshooting
- **If Port 8080 is still in use:** 
`kill $(lsof -nP -tiTCP:8080 -sTCP:LISTEN)`.
- **To check if its running on there:**
`lsof -nP -iTCP:5173 -sTCP:LISTEN`.

- **Vite uses a different port (e.g. 5174):** restart the API with `FRONTEND_ORIGIN=http://localhost:5174 mvn spring-boot:run`.
- **Port 5433 already in use:** stop whatever holds it, or change the port in `docker-compose.yml` and `DB_URL`.
- **Search returns nothing right after indexing:** wait a few seconds and retry.
