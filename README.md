# Test Case Generator

LLM-powered Spring Boot application that generates JUnit, Mockito, and REST Assured test suites from requirements, OpenAPI specs, and Java code

## Features

| # | Feature | How it works |
|---|---------|---------------|
| 1 | Requirement → JUnit tests | Paste a user story / requirement → generates a JUnit 5 + Mockito test class (positive, negative, boundary, security, concurrency cases) |
| 2 | OpenAPI/Swagger → REST Assured tests | Paste a spec (JSON/YAML) → generates integration tests: happy path, auth, validation, boundary, negative |
| 3 | Java code → JUnit + Mockito tests | Paste a Controller/Service/Repository → generates a test class mocking all collaborators |
| 4 | Security test generation | Describe an endpoint → generates SQLi, JWT, CSRF, XSS, broken-auth, rate-limit tests |
| 5 | Chat with your requirements | Paste context + ask a free-form question (e.g. "which edge cases am I missing?") |

Every generated test can be copied or downloaded as a standalone `.java` file directly from the UI.

## Tech stack

- **Backend:** Java 21, Spring Boot 3.5 (Web, Validation)
- **AI:** **Spring AI 1.1** `ChatClient`, pointed at Groq's free tier (default model `llama-3.3-70b-versatile`)
- **Frontend:** Static HTML/CSS/JS (no build step) served from `src/main/resources/static`

Spring AI reaches Groq through its OpenAI-compatible client — same client, different `base-url` and model.
Because Spring AI abstracts the provider, switching to OpenAI, OpenRouter, or a local Ollama model is a
**config-only change** (`spring.ai.openai.*` in `application.yml`); no Java code changes.

## Getting started

**1. Get a free Groq API key** at https://console.groq.com/keys (no credit card required).

**2. Configure it:**
copy .env.example .env
edit .env and paste your key

**3. Run it:**
docker compose up --build
Then open **http://localhost:8081**.

## API reference

All endpoints accept/return JSON.

GET  /api/health
POST /api/generate/requirements   { "input": "<requirement text>" }
POST /api/generate/openapi        { "input": "<OpenAPI/Swagger spec>" }
POST /api/generate/code           { "input": "<Java source>" }
POST /api/generate/security       { "input": "<endpoint description>" }
POST /api/chat                    { "input": "<context, optional>", "question": "<question>" }



