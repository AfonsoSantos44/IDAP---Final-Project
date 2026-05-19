# IDAP - Intelligent Damage Analysis Platform

IDAP is a Kotlin/Spring Boot backend for managing accident damage analysis cases. It provides user registration, cookie-based authentication, role-based authorization, and case management endpoints backed by PostgreSQL through JDBI.

This repository is organized as a multi-module Gradle project intended for a university final project. The current implementation focuses on backend API behavior, authentication, authorization, and persistence for users, session tokens, and accident cases.

## System Goal

The goal of IDAP is to support accident investigation workflows by storing users and accident cases in a structured backend. The implemented API currently allows authenticated users to create and manage their own cases, while administrators can manage users and inspect cases globally.

Planned domain areas such as evidence, images, analyses, measurements, and reports are represented in the SQL schema, but they are not yet exposed through HTTP endpoints.

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

## Configuration

The application reads the following environment variables through `application.properties`:

| Environment variable | Application property | Default | Description |
| --- | --- | --- | --- |
| `PORT` | `server.port` | `8080` | HTTP port used by Spring Boot |
| `DB_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/idap` | PostgreSQL JDBC URL |
| `DB_USER` | `spring.datasource.username` | `postgres` | Database username |
| `DB_PASSWORD` | `spring.datasource.password` | `postgres` | Database password |
| `SESSION_COOKIE_SECURE` | `idap.session.cookie.secure` | `false` | Whether the session cookie is marked `Secure` |

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

The OpenAPI description is available in `docs/IDAP.yaml`.

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
- PostgreSQL persistence for users, tokens, and cases
- automated tests for authentication, authorization, and token persistence

Current limitations:

- database schema management is manual through SQL scripts; no migration tool is configured
- administrator promotion is manual through `set-admin.sql`
- evidence, image, analysis, measurement, and report tables exist in SQL but do not yet have HTTP endpoints
- there is no pagination or filtering for user/case listing endpoints
- there is no refresh-token flow or session management endpoint beyond logout
- the OpenAPI file is maintained manually and is not generated from code

Planned or possible future work:

- add API endpoints for evidence, images, analyses, measurements, and reports
- introduce database migrations
- add pagination and search/filtering
- add operational deployment documentation
- generate OpenAPI documentation automatically from the application
