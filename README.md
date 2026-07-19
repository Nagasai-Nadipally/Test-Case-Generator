# Test Case Generator
LLM-powered Spring Boot application that generates JUnit, Mockito, and REST Assured test suites from requirements, OpenAPI specs, and Java.
**Live Demo URL:** https://test-case-generator-zr65.onrender.com/

## What it does
- **A requirement or user story →**a JUnit 5 + Mockito test class covering the happy path, the failure cases, the boundaries, and the edge cases you'd probably forget at 5pm on a Friday.
- **An OpenAPI or Swagger spec →** REST Assured integration tests that hit every documented response: the 200s, the auth failures, the validation errors, the conflicts.
- **A Java class, controller, service, repository →** a test class that mocks its collaborators and exercises every public method, including the null and exception paths.
- **A description of an endpoint →** security tests that actually try to break it: SQL injection, XSS, tampered JWTs, missing auth, rate limiting.
- Or just ask it something. Paste your requirements and ask "what edge cases am I missing?" you get an answer, not code.
Anything it generates, you can copy or download as a ready-to-drop-in .java file.

## Tech stack
- **Backend:** Java 21, Spring Boot 3.5 (Web, Validation)
- **AI:** **Spring AI 1.1** `ChatClient`, pointed at Groq's free tier (default model `llama-3.3-70b-versatile`)
- **Frontend:** Static HTML/CSS/JS (no build step) served from `src/main/resources/static`
- Spring AI reaches Groq through its OpenAI-compatible client, same client, different `base-url` and model.

## Getting started
- **Get a free Groq API key** at https://console.groq.com/keys (no credit card required).
- **Configure it:**
copy .env.example .env
edit .env and paste your key
- **Run it:**
docker compose up --build
Then open **http://localhost:8081**.

## API reference
All endpoints accept/return JSON.
- GET  /api/health
- POST /api/generate/requirements   { "input": "<requirement text>" }
- POST /api/generate/openapi        { "input": "<OpenAPI/Swagger spec>" }
- POST /api/generate/code           { "input": "<Java source>" }
- POST /api/generate/security       { "input": "<endpoint description>" }
- POST /api/chat                    { "input": "<context, optional>", "question": "<question>" }



