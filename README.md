# IDAP - Intelligent Damage Analysis Platform

IDAP is a Kotlin/Spring Boot backend for managing accident damage analysis cases. It provides user registration, cookie-based authentication, role-based authorization, case management, and accident workflow data endpoints backed by PostgreSQL through JDBI.

This repository is organized as a multi-module Gradle project intended for a university final project. The current implementation focuses on backend API behavior, authentication, authorization, and persistence for users, session tokens, accident cases, and accident investigation data.

## System Goal

The goal of IDAP is to support accident investigation workflows by storing users, accident cases, context data, vehicles, damages, evidence, analyses, measurements, comparisons, conclusions, and reports in a structured backend. The implemented API allows authenticated users to create and manage their own cases and related workflow data, while administrators can manage users and inspect cases globally.

## Architecture Overview

The backend follows a layered modular architecture:

```text
Client
  |
  v
http module - REST controllers and DTOs
  |
  v
services module - business rules and validation
  |
  v
repository module - persistence interfaces
  |
  v
repository-jdbi module - SQL/JDBI implementation
  |
  v
PostgreSQL
```

The `domain` module contains shared entities and value types used across the layers. The `app` module starts Spring Boot, configures security, creates infrastructure beans, and wires the modules together.

## Modules

### app

Spring Boot entry point. It contains:

- `IdapApp`, the application bootstrap class
- Spring configuration in `AppConfig`
- Spring Security configuration in `SecurityConfig`
- `SessionTokenAuthenticationFilter`, which reads the `idap_session` cookie
- application properties
- HTTP/security and persistence integration tests

### domain

Shared domain model. It contains entities and types such as:

- `User`
- `UserRole`
- `Token`
- `SecurityPrincipal`
- `AccidentCase`
- additional accident analysis domain classes used by the database model

### http

REST API layer. It contains:

- `UserController`
- `CaseController`
- URI constants
- request and response DTOs
- API problem response definitions

Controllers are responsible for HTTP status codes, request/response mapping, and authorization checks that depend on the authenticated principal.

### services

Business logic layer. It contains:

- user registration validation
- password security checks
- login/session token creation
- token validation and deletion
- case validation and case business operations

Services use repository interfaces instead of depending directly on JDBI.

### repository

Persistence abstraction layer. It defines repository and transaction interfaces:

- `RepositoryUser`
- `RepositoryCase`
- `Transaction`
- `TransactionManager`

This module lets services depend on contracts rather than a specific database technology.

### repository-jdbi

PostgreSQL/JDBI persistence implementation. It contains:

- JDBI repository implementations
- row mappers
- transaction manager implementation
- SQL scripts for schema creation, schema cleanup, and admin promotion

## Requirements

- JDK 21
- Gradle wrapper included in the repository
- PostgreSQL for running the application
- A database created for the application, for example `idap`

The test suite also uses H2 for a small persistence integration test, but the running application uses PostgreSQL.

## Database Configuration

The default application properties expect PostgreSQL at:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/idap
spring.datasource.username=postgres
spring.datasource.password=postgres
```

Create the database before starting the application, then apply the schema:

```bash
psql -U postgres -d idap -f code/jvm/repository-jdbi/src/sql/create-schema.sql
```

To remove all schema objects in a development database:

```bash
psql -U postgres -d idap -f code/jvm/repository-jdbi/src/sql/drop-tables.sql
```

To promote a user named `admin` to administrator:

```bash
psql -U postgres -d idap -f code/jvm/repository-jdbi/src/sql/set-admin.sql
```

## Image Storage

Image bytes (evidence photos and generated annotated comparison images) are **not** stored
in PostgreSQL. They live in S3-compatible object storage (MinIO), and the relational tables
keep only the object key:

- `image_evidence.file_path` holds the object key of an uploaded evidence image
- `measurement.comparison_image_path` holds the object key of a generated comparison image

This keeps the PostgreSQL tables and backups small while still allowing the API to serve the
original and annotated images on demand.

Start MinIO locally with the bundled Compose file:

```bash
docker compose up -d
```

This exposes the S3 API on `http://localhost:9000` and the web console on
`http://localhost:9001` (default credentials `minioadmin` / `minioadmin`). The application
creates the `idap-images` bucket automatically on first upload.

Evidence images are uploaded as `multipart/form-data`:

```http
PUT /api/evidence/{evidenceId}/image
Content-Type: multipart/form-data
```

with a `file` part (the image) and an optional `metadata` text part. The backend reads the
image dimensions, stores the bytes in object storage, and persists the key plus dimensions.
The raw bytes are served back from:

```http
GET /api/evidence/{evidenceId}/image/content
GET /api/measurements/{measurementId}/comparison-image
```

## Configuration

The application reads the following environment variables through `application.properties`:

| Environment variable | Application property | Default | Description |
| --- | --- | --- | --- |
| `PORT` | `server.port` | `8080` | HTTP port used by Spring Boot |
| `DB_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/idap` | PostgreSQL JDBC URL |
| `DB_USER` | `spring.datasource.username` | `postgres` | Database username |
| `DB_PASSWORD` | `spring.datasource.password` | `postgres` | Database password |
| `SESSION_COOKIE_SECURE` | `idap.session.cookie.secure` | `false` | Whether the session cookie is marked `Secure` |
| `MEASUREMENT_PYTHON` | `idap.measurement.python-executable` | `python` | Python executable used by the image measurement engine |
| `MEASUREMENT_SCRIPT` | `idap.measurement.script-path` | `Image_analysis/measurement_engine.py` | Python script used for ruler/damage image processing |
| `MEASUREMENT_OUTPUT_DIR` | `idap.measurement.output-dir` | `build/idap-measurements` | Directory where annotated comparison images are generated |
| `MEASUREMENT_TIMEOUT_SECONDS` | `idap.measurement.timeout-seconds` | `90` | Timeout for each image processing run, including automatic OCR |
| `TESSERACT_CMD` | `idap.measurement.tesseract-executable` | empty | Optional path to the Tesseract executable used for automatic ruler-label OCR |
| `WEATHERAPI_KEY` | `idap.weather.api-key` | empty | WeatherAPI key used to fetch current weather for a case |
| `WEATHERAPI_BASE_URL` | `idap.weather.base-url` | `https://api.weatherapi.com/v1` | WeatherAPI base URL |
| `GOOGLE_MAPS_API_KEY` | `idap.scene.google-api-key` | empty | Google Maps API key used for scene metadata |
| `GOOGLE_STREET_VIEW_METADATA_URL` | `idap.scene.street-view-metadata-url` | `https://maps.googleapis.com/maps/api/streetview/metadata` | Google Street View metadata URL |
| `GOOGLE_GEOCODING_URL` | `idap.scene.geocoding-url` | `https://maps.googleapis.com/maps/api/geocode/json` | Google reverse geocoding URL |
| `GOOGLE_ELEVATION_URL` | `idap.scene.elevation-url` | `https://maps.googleapis.com/maps/api/elevation/json` | Google elevation URL used for rough slope estimation |
| `MINIO_ENDPOINT` | `idap.storage.endpoint` | `http://localhost:9000` | S3-compatible object storage endpoint for image bytes |
| `MINIO_ACCESS_KEY` | `idap.storage.access-key` | `minioadmin` | Object storage access key |
| `MINIO_SECRET_KEY` | `idap.storage.secret-key` | `minioadmin` | Object storage secret key |
| `MINIO_BUCKET` | `idap.storage.bucket` | `idap-images` | Bucket where evidence and comparison images are stored |
| `MAX_UPLOAD_SIZE` | `spring.servlet.multipart.max-file-size` / `max-request-size` | `25MB` | Maximum size of an uploaded image |

For production-like HTTPS deployments, set `SESSION_COOKIE_SECURE=true`.

## Running the Application

From the repository root:

```bash
./gradlew bootRun
```

On Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

By default, the API starts on `http://localhost:8080`.

## Running Tests

Run the full test suite:

```bash
./gradlew test
```

Run the full verification lifecycle, including configured ktlint checks:

```bash
./gradlew clean check
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

## Authentication Model

IDAP uses cookie-based session authentication. It does not use JWT or Bearer authentication.

Login is performed with:

```http
POST /api/users/login
```

On successful login, the server returns an opaque random session token in an HttpOnly cookie:

```text
idap_session=<token>; Path=/; Max-Age=86400; HttpOnly; SameSite=Lax
```

The database stores only a SHA-256 hash of the token. The raw token is returned only to the client in the cookie. Session tokens expire after 24 hours. Logout deletes the stored token and clears the cookie.

Authorization is role based:

- anonymous users can register and log in
- regular users can access their own profile and cases
- administrators can list/delete users and access cases globally

## Main Endpoints

### Users

| Method | Path | Authentication | Description |
| --- | --- | --- | --- |
| `POST` | `/api/users` | Public | Register a user |
| `POST` | `/api/users/login` | Public | Log in and receive the `idap_session` cookie |
| `POST` | `/api/users/logout` | Authenticated | Delete the current session token and clear the cookie |
| `GET` | `/api/users/me` | Authenticated | Read the current authenticated user |
| `GET` | `/api/users/{id}` | Owner or admin | Read a user profile |
| `GET` | `/api/users` | Admin | List users |
| `DELETE` | `/api/users/{id}` | Admin | Delete a user |

### Cases

| Method | Path | Authentication | Description |
| --- | --- | --- | --- |
| `GET` | `/api/cases` | Authenticated | Regular users see their own cases; admins see all cases |
| `POST` | `/api/cases` | Authenticated | Create a case; admin may provide `userId` |
| `GET` | `/api/cases/{id}` | Case owner or admin | Read a case |
| `PUT` | `/api/cases/{id}` | Case owner or admin | Update case status and/or description |
| `DELETE` | `/api/cases/{id}` | Case owner or admin | Delete a case |
| `GET` | `/api/users/{userId}/cases` | Same user or admin | List cases for a specific user |

### Accident Workflow Data

| Method | Path | Authentication | Description |
| --- | --- | --- | --- |
| `GET`, `PUT`, `DELETE` | `/api/cases/{caseId}/weather` | Case owner or admin | Read, refresh from WeatherAPI, or delete case weather conditions |
| `GET`, `PUT`, `DELETE` | `/api/cases/{caseId}/scene` | Case owner or admin | Read, refresh from Google Maps metadata, or delete accident scene context |
| `GET`, `POST` | `/api/cases/{caseId}/vehicles` | Case owner or admin | List or create vehicles involved in a case |
| `GET`, `PUT`, `DELETE` | `/api/vehicles/{vehicleId}` | Case owner or admin | Read, update, or delete a vehicle |
| `GET`, `POST` | `/api/vehicles/{vehicleId}/damages` | Case owner or admin | List or create damages for a vehicle |
| `GET`, `PUT`, `DELETE` | `/api/damages/{damageId}` | Case owner or admin | Read, update, or delete a damage record |
| `GET`, `POST` | `/api/cases/{caseId}/evidence` | Case owner or admin | List or create evidence for a case |
| `GET`, `PUT`, `DELETE` | `/api/evidence/{evidenceId}` | Case owner or admin | Read, update, or delete evidence |
| `GET`, `DELETE` | `/api/evidence/{evidenceId}/image` | Case owner or admin | Read or delete image evidence metadata |
| `PUT` | `/api/evidence/{evidenceId}/image` | Case owner or admin | Upload/replace the image (`multipart/form-data`); stored in object storage |
| `GET` | `/api/evidence/{evidenceId}/image/content` | Case owner or admin | Download the raw evidence image bytes |
| `GET` | `/api/measurements/{measurementId}/comparison-image` | Case owner or admin | Download the generated annotated comparison image |
| `GET`, `POST` | `/api/cases/{caseId}/analyses` | Case owner or admin | List or create analyses for a case |
| `GET`, `DELETE` | `/api/analyses/{analysisId}` | Case owner or admin | Read or delete an analysis |
| `GET`, `PUT` | `/api/analyses/{analysisId}/images` | Case owner or admin | List or attach evidence images to an analysis |
| `DELETE` | `/api/analyses/{analysisId}/images/{evidenceId}` | Case owner or admin | Detach evidence from an analysis |
| `GET`, `POST` | `/api/analyses/{analysisId}/measurements` | Case owner or admin | List measurements or run the automatic image measurement pipeline |
| `GET`, `DELETE` | `/api/measurements/{measurementId}` | Case owner or admin | Read or delete a generated measurement |
| `GET`, `POST` | `/api/analyses/{analysisId}/damage-comparisons` | Case owner or admin | List or create damage comparisons |
| `GET`, `PUT`, `DELETE` | `/api/damage-comparisons/{comparisonId}` | Case owner or admin | Read, update, or delete a damage comparison |
| `GET`, `PUT`, `DELETE` | `/api/analyses/{analysisId}/conclusion` | Case owner or admin | Read, replace, or delete an analysis conclusion |
| `GET`, `POST` | `/api/analyses/{analysisId}/reports` | Case owner or admin | List or create generated report records |
| `GET`, `PUT`, `DELETE` | `/api/reports/{reportId}` | Case owner or admin | Read, update, or delete a report record |

The OpenAPI description is available in `docs/IDAP.yaml`. It currently documents the user and case endpoints; the accident workflow endpoints should be added there as the next documentation pass.

Weather and scene data are fetched from external providers. To create or refresh the scene, the client sends coordinates:

```http
PUT /api/cases/{caseId}/scene
```

```json
{
  "latitude": 38.736946,
  "longitude": -9.142685
}
```

The backend uses Google Street View metadata, reverse geocoding, and elevation data to fill the scene fields. Weather can then be refreshed with the stored scene coordinates:

```http
PUT /api/cases/{caseId}/weather
```

```json
{}
```

The client may also provide explicit weather coordinates:

```json
{
  "latitude": 38.736946,
  "longitude": -9.142685
}
```

Measurement creation is image-driven. The client registers evidence and uploads the corresponding images first, then calls:

```http
POST /api/analyses/{analysisId}/measurements
```

with a body such as:

```json
{
  "evidenceId": 12,
  "damageId": 7,
  "comparisonEvidenceId": 13,
  "knownTickDistanceCm": 1.0,
  "primarySelection": {
    "x1": 370,
    "y1": 515,
    "x2": 1450,
    "y2": 650
  },
  "comparisonSelection": {
    "x1": 1000,
    "y1": 120,
    "x2": 1160,
    "y2": 220
  }
}
```

All coordinates use pixels from the original uploaded image. The analyst must provide
`primarySelection`; it may be a rectangle or a point. `comparisonSelection` is required
when `comparisonEvidenceId` is present. A calibration may also include `rulerRegion` to
restrict where the engine searches for the ruler.

The engine perspective-corrects the detected ruler and maps pixels to centimetres.
Automatic OCR reads the ruler labels and validates the resulting scale against the
detected tick spacing. The analyst normally provides only the damage rectangle for
each image. Manual ruler reference points remain available as a fallback for damaged,
covered, blurred, or otherwise unreadable rulers. The backend stores the selected
region, its centre height, minimum and maximum ruler values, physical pixel scale,
calibration method, confidence, and the generated annotated image. Comparison images
are resized to the same physical centimetres-per-pixel scale and vertically aligned
at the same ruler value.

If automatic OCR cannot read a particular ruler, the same request may optionally
include two manual reference points:

```json
"primaryCalibration": {
  "referencePoints": [
    { "x": 320, "y": 493, "valueCm": 80 },
    { "x": 320, "y": 959, "valueCm": 70 }
  ]
}
```

Install the Python dependencies with:

```powershell
python -m pip install -r Image_analysis/requirements.txt
```

The image-analysis module is organized by responsibility:

- `measurement_engine.py` reads the JVM request and orchestrates processing
- `measurement_processing.py` runs the per-image measurement workflow
- `ruler_detection.py` detects and perspective-corrects rulers and ticks
- `ruler_calibration.py` performs OCR and pixel-to-centimetre calibration
- `measurement_geometry.py` calculates damage heights and ruler positions
- `comparison_rendering.py` annotates, scales, and aligns comparison images
- `image_inputs.py` validates and normalizes image coordinates

The project currently manages its development schema through `create-schema.sql`
instead of incremental migrations. If the database was created before these
measurement fields were added, recreate the schema:

```powershell
psql -U postgres -d idap -f code/jvm/repository-jdbi/src/sql/drop-tables.sql
psql -U postgres -d idap -f code/jvm/repository-jdbi/src/sql/create-schema.sql
```

## Error Responses

The API uses `application/problem+json` responses for authentication, authorization, validation, and not-found errors. Current problem types are documented under `docs/problems`.

Problem response bodies currently include:

```json
{
  "type": "https://github.com/AfonsoSantos44/IDAP---Final-Project/docs/problems/invalid-token",
  "title": "Invalid token"
}
```

The HTTP status code is carried by the response status.

## Current Limitations and Future Work

Implemented:

- user registration
- login/logout with cookie-based session tokens
- token hashing and expiration
- role-based user and case authorization
- user listing/deletion for administrators
- case creation, listing, reading, updating, and deletion
- accident workflow endpoints for weather, scene, vehicles, damages, evidence, images, analyses, measurements, comparisons, conclusions, and reports
- WeatherAPI-backed weather refresh and Google Maps-backed scene refresh
- Python-backed ruler perspective correction, calibrated damage measurement, and physically aligned comparison images
- PostgreSQL persistence for users, tokens, cases, and accident workflow data
- automated tests for authentication, authorization, and token persistence

Current limitations:

- database schema management is manual through SQL scripts; no migration tool is configured
- administrator promotion is manual through `set-admin.sql`
- WeatherAPI and Google Maps API keys are required to refresh weather and scene data
- automatic OCR calibration requires the RapidOCR models to be available; they are downloaded by RapidOCR on first use, while analyst reference points remain available for unreadable rulers
- there is no pagination or filtering for user/case listing endpoints
- there is no refresh-token flow or session management endpoint beyond logout
- the OpenAPI file is maintained manually and is not generated from code

Planned or possible future work:

- introduce database migrations
- add pagination and search/filtering
- add operational deployment documentation
- generate OpenAPI documentation automatically from the application
