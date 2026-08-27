# Auth Service | Shopping Cart Project

The **Auth Service** is a dedicated identity and authentication microservice. It acts as the security backbone of the distributed architecture, responsible for secure user onboarding, credential verification, and session management using stateless JSON Web Tokens (JWT).

## Features & Architecture

This service is built with an enterprise-grade technology stack and strict security practices:
* **Java 21 & Spring Boot 3.2.0**: The core framework driving the microservice.
* **PostgreSQL & H2**: Uses PostgreSQL as the primary persistence layer and an in-memory H2 database for rapid unit testing.
* **JWT & HttpOnly Cookies**: Issues stateless JWT Access Tokens and highly secure, XSS-proof `HttpOnly` Refresh Tokens.
* **Role-Based Access Control**: Granular RBAC supporting `CUSTOMER`, `SELLER`, and `ADMIN` roles, with automated admin seeding on startup.

## Local Development & Setup

To run this service locally, you must create a `.env` file in the root directory alongside the `docker-compose.yml` file. 

Define the following environment variables:
* `DB_URL`: The PostgreSQL JDBC connection string.
* `DB_NAME`: The target database name.
* `DB_USERNAME`: The database user.
* `DB_PASSWORD`: The secure database password.
* `JWT_SECRET`: The Base64 encoded secret key for token signing.

Once your `.env` file is ready, you can start the service and its isolated database using Docker Compose:
`docker compose up -d`

## Core API Endpoints

The service exposes the following public endpoints for the API Gateway to route traffic to:
* `POST /api/v1/auth/register`: Registers a new user and returns an authentication payload.
* `POST /api/v1/auth/login`: Authenticates credentials and issues a token pair.
* `POST /api/v1/auth/refresh`: Rotates the user's session by issuing a new JWT from a valid refresh cookie.
* `POST /api/v1/auth/logout`: Revokes the active refresh token family and clears the user's session.