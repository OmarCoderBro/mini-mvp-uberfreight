# Uber Freight Freight Search & Indexing MVP

## Project goal

Build a small, recruiter-demoable shipment search application that demonstrates a basic indexing pipeline.

A user should be able to search and filter synthetic freight listings. PostgreSQL is the source of truth. Elasticsearch is a derived search index. A small indexing script copies shipment records from PostgreSQL into Elasticsearch.

This is an independent portfolio project inspired by the public City Scholars internship description. It is not affiliated with or endorsed by Uber Freight. Use synthetic data only.

## Product scope

### Required user flow

1. Load a few hundred synthetic shipment listings into PostgreSQL.
2. Run a repeatable indexing command that reads records from PostgreSQL and upserts them into Elasticsearch.
3. Search and filter indexed shipments through a FastAPI endpoint.
4. Use a small React and TypeScript page to enter a query, choose filters, and view matching shipments.
5. Show useful empty, loading, and error states.

### Shipment fields

Use a compact schema with only the fields needed for the demo:

- id
- origin_city and origin_state
- destination_city and destination_state
- pickup_date
- equipment_type
- rate_usd
- weight_lbs
- status
- updated_at

Generate fictional cities, dates, rates, weights, and statuses. Label demo data as synthetic in the UI and README.

### Search behavior

Support:

- Free-text query across origin and destination city/state.
- Exact or prefix filters for origin and destination.
- Equipment type filter.
- Minimum and maximum rate filters.
- Pagination with a small default page size.

Return matching records, total match count, page, and page size. Validate filter values and pagination inputs. Keep the search implementation in Elasticsearch; do not load every shipment into Python and filter in memory.

## Technology choices

- Frontend: React, TypeScript, and Vite.
- API: Java and Spring Boot.
- Source database: PostgreSQL.
- Search index: Elasticsearch.
- Local development: Docker Compose for PostgreSQL and Elasticsearch.

Use only these technologies unless the user asks to change the stack. Keep the frontend as a normal static Vite app so it can be deployed to Netlify later. The complete local demo may use the API and databases through Docker Compose.

Do not add Spark, Flink, Kafka, Hive, Kubernetes, AWS, or additional infrastructure. The internship description mentions stream or batch processing technologies, but this MVP demonstrates a small batch indexing pipeline without those systems.

## Architecture and data ownership

- PostgreSQL owns the canonical shipment records.
- Elasticsearch contains documents derived from PostgreSQL and serves search requests.
- The indexing script may rebuild the complete small index. Make indexing repeatable: running it twice must not create duplicate documents.
- Use the PostgreSQL shipment id as the Elasticsearch document id.
- Keep database access, indexing, API routes, and search query construction in clearly named modules.
- Do not create multiple network services for the API and indexer. The indexer is a command-line script for this MVP.

## Suggested repository layout

Adapt to the existing repository if one exists. Do not move or rename existing files without a clear need.

- frontend/ — Vite React TypeScript application
- backend/ — Spring Boot (Java) Maven project
  - src/main/java/com/uberfreight/search/ShipmentSearchApplication.java — Spring Boot app entry point
  - src/main/java/com/uberfreight/search/api/ — API routes (health, search)
  - src/main/java/com/uberfreight/search/search/ — Elasticsearch client, query builder, search service
  - src/main/java/com/uberfreight/search/model/ — request/response records
  - src/main/java/com/uberfreight/search/scripts/ — SeedData and ReindexRunner standalone classes (run via Maven exec)
  - src/test/java/... — focused API, query, and indexer tests
- docker-compose.yml — local PostgreSQL and Elasticsearch
- .env.example — names of required local environment variables with safe placeholders
- .gitignore — exclude .env, virtual environments, caches, node_modules, and build output
- README.md — setup, architecture, endpoints, demo flow, and limitations

Keep the folder structure smaller if a simpler layout is clearer. Do not create empty modules or placeholder abstractions.

## API contract

Implement only the endpoints needed by the user flow.

### GET /health

Return a small success response for checking that the API is running. If practical, report whether PostgreSQL and Elasticsearch are reachable without exposing credentials.

### GET /shipments/search

Accept query parameters for q, origin, destination, equipment_type, min_rate, max_rate, page, and page_size.

Return JSON with:

- items: matching shipment records
- total: number of matches
- page
- page_size

Keep one consistent response schema and document it in the README.

Do not add a public write endpoint for editing shipments. Seed and reindex data through local scripts.

## Frontend expectations

Build one straightforward page:

- Search box.
- Origin, destination, equipment, and rate filters.
- Search button or responsive submit behavior.
- Results count and compact shipment result rows.
- Loading, no-results, and API-error states.
- Clear synthetic-data label.
- No login screen or complex navigation.

Keep styling readable and responsive. Avoid spending time on animations, elaborate branding, charts, or a multi-page design.

## Reliability and data handling

- Keep credentials and local connection strings in environment variables.
- Never commit .env files, API keys, or real customer or carrier data.
- Validate API input and return understandable 4xx errors for invalid filters.
- Handle unavailable PostgreSQL or Elasticsearch with useful startup or health errors.
- Use stable field mappings in Elasticsearch: identifiers and categorical filters should be exact-match fields; rates and weights numeric; dates date fields; searchable location text analyzed appropriately.
- Make indexer failures visible. Do not silently report success if records failed to index.
- Avoid claiming production scale, real-time guarantees, or production-grade security.

## Tests and quality bar

Add focused tests for meaningful behavior:

- Search parameter validation and response shape.
- Elasticsearch query construction for text, filters, and rate ranges.
- Reindexing uses stable document ids and can be run repeatedly without duplicates.
- A small API test for health or search behavior using a mock where external services are not available.

Do not write tests that only mirror implementation details. Do not add broad test infrastructure before the core flow works.

Before finishing a task, run the relevant tests or checks available in the repository. Report which commands ran and whether they passed. Do not claim a check passed unless it was actually run.

## Working instructions for Claude Code

- Start by inspecting the repository and reporting what already exists.
- Before making broad changes, propose a short implementation plan and wait for approval.
- Work in small vertical slices: local services and data, indexer, search API, then UI.
- Do not scaffold every feature in one turn.
- Preserve existing working code and conventions.
- Ask before adding a dependency that is not in the chosen stack, changing the architecture, deleting files, or expanding product scope.
- Prefer simple, readable code over generic frameworks or premature abstractions.
- Keep changes focused on the current task. Do not reformat unrelated files.
- When a task is complete, summarize changed files, behavior, commands run, and any remaining issue.
- If requirements are ambiguous, choose the smallest implementation consistent with this document and state the assumption.

## Recommended implementation sequence

1. Inspect the repo and write a concise plan.
2. Add Docker Compose and verify PostgreSQL and Elasticsearch start locally.
3. Create the shipment schema and a script that loads a few hundred synthetic records.
4. Implement the idempotent PostgreSQL-to-Elasticsearch reindex script.
5. Implement and test the search endpoint and input validation.
6. Build the single-page React search interface.
7. Verify the complete demo flow and document exact setup commands.

Finish the required flow before considering any optional performance measurement.