package com.testgenai.service;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    private static final String COMMON_RULES = """
            Rules:
            - Output ONLY valid Java code (or the requested artifact), no markdown fences, no prose before or after.
            - Include realistic package declaration `com.testgenai.generated;` and necessary imports.
            - Include clear comments above each test explaining what it verifies.
            - Cover positive, negative, boundary, and where relevant, security and concurrency cases.
            - Prefer JUnit 5 (org.junit.jupiter.api) idioms: @Test, @ParameterizedTest, @DisplayName, assertThrows, etc.
            """;

    public String requirementSystemPrompt() {
        return """
                You are a senior SDET (Software Development Engineer in Test) with 15 years of experience
                writing enterprise-grade automated test suites for Java/Spring Boot systems.
                Given a natural-language requirement or user story, you extract actors, business rules,
                acceptance criteria, edge cases, validation rules and dependencies, then produce a complete
                JUnit 5 test class (using Mockito where a service/repository dependency is implied) covering:
                positive cases, negative cases, boundary cases, security cases, and concurrency cases where relevant.
                """ + COMMON_RULES;
    }

    public String requirementUserPrompt(String requirementText) {
        return "Requirement / User Story:\n" + requirementText + """

                Produce a single JUnit 5 test class named GeneratedRequirementTest that exercises this
                requirement end-to-end at the service layer (mock any repository/external dependency with Mockito).

                Important correctness rule: never call Mockito's when(...) or verify(...) on the class under
                test itself (the object annotated @InjectMocks or otherwise constructed as the real thing
                being tested) — that throws MissingMethodInvocationException at runtime. Only stub methods
                on objects annotated @Mock. If a method under test needs to look up existing state (e.g. an
                existing booking, order, or record) via some collaborator, explicitly declare and @Mock that
                collaborator (e.g. a repository), even if the requirement text doesn't name it outright, and
                stub the lookup on that mock instead.

                Important correctness rule: enable Mockito with @ExtendWith(MockitoExtension.class) on the
                test class — never @MockitoSettings, which is only for tuning strictness and defaults to
                STRICT_STUBS. Under strict stubs, any stub that a given test never actually invokes throws
                UnnecessaryStubbingException and fails the test. So in each test method, stub only the
                collaborators that code path will really call: if the method short-circuits (e.g. the record
                isn't found, so a second lookup never happens), do not stub that second lookup in that test.
                """;
    }

    public String openApiSystemPrompt() {
        return """
                You are a senior API test automation engineer. Given an OpenAPI/Swagger specification
                (JSON or YAML), you generate a complete REST Assured integration test class in Java that
                covers: happy-path requests for each documented operation, authentication/authorization
                tests, request validation tests (missing/invalid fields), boundary tests, and negative
                tests (4xx/5xx expectations) based on the schema.
                """ + COMMON_RULES;
    }

    public String openApiUserPrompt(String specText) {
        return "OpenAPI/Swagger specification:\n" + specText + """

                Produce a single REST Assured test class named GeneratedApiTest. Assume the base URI is
                read from a constant `BASE_URI = "http://localhost:8080"` that the user can edit.

                Important correctness rule: a plain given()....when().post(...) call in REST Assured
                returns a Response object and does NOT throw an exception for HTTP error status codes
                (401, 403, 404, etc). Never wrap such a call in assertThrows(...ResponseException.class, ...)
                to test an error status. Instead, for every test including auth-failure cases, capture the
                Response and assert on it with assertEquals(expectedStatusCode, response.getStatusCode()) or
                response.then().statusCode(expectedStatusCode), consistently with all other tests in the class.

                Important correctness rule: for any test verifying a state-dependent response (for example
                409 Conflict for "already exists" or "already processed" scenarios), do not assume that
                state already exists on the server. Either perform the real setup call(s) needed to legitimately
                reach that state before the assertion (e.g. call the same endpoint once first to create the
                conflict), or, if that's impractical, use a distinctly-named path/fixture id (e.g.
                "PRE_EXISTING_CONFLICT_ID") and add a comment stating this test assumes that fixture is
                pre-seeded in the test environment. Never write a comment like "assume already done" without
                either creating that state in the test or clearly flagging it as an external fixture dependency.

                Important correctness rule: 401 and 403 are different failures and need different requests.
                401 means unauthenticated, so test it with a missing or invalid token. 403 means
                authenticated but not permitted, so it must use a VALID token belonging to a user who lacks
                the required role or ownership - declare a separate constant for that (e.g.
                VALID_TOKEN_WITHOUT_PERMISSION). Reusing the invalid/missing token from the 401 test in a
                403 test is wrong: that request would return 401, not 403.
                """;
    }

    public String codeSystemPrompt() {
        return """
                You are a senior Java engineer specializing in unit testing Spring Boot Controllers,
                Services, and Repositories. Given a Java source file, you generate a complete JUnit 5 +
                Mockito test class that mocks all collaborators, covers every public method, and includes
                edge cases (nulls, empty collections, exceptions thrown by dependencies) plus a short
                coverage summary as a comment block at the top of the file.
                """ + COMMON_RULES;
    }

    public String codeUserPrompt(String codeText) {
        return "Java source file to test:\n" + codeText + """

                Produce a single JUnit 5 + Mockito test class. Name it by taking the class under test's
                name and appending `Test` (e.g. UserService -> UserServiceTest). Infer that name from the
                source and use it as the class name.

                Rule 1 - writing correct assertions. Compute every expected value as an independent
                literal; never re-run the implementation's own formula or branching logic inside the test
                body, since a test that derives its expectation the same way the production code does will
                pass even when that logic is wrong. Because those literals are hardcoded they must be
                arithmetically correct, so immediately above each one write a comment tracing the
                calculation step by step - applying operations in the same order the production code does,
                including any guard clause or early return the chosen inputs would hit - and derive the
                literal from that worked example. For java.math.BigDecimal, never use plain
                assertEquals(expected, actual): BigDecimal.equals() is scale-sensitive ("0" and "0.00" are
                numerically equal but not .equals()-equal), so use assertEquals(0, expected.compareTo(actual)).
                For example:
                    // 100.00 * 0.02 = 2.00; 2.00 * 10 days = 20.00; 20.00 <= 50.00 cap, so no capping
                    assertEquals(0, new BigDecimal("20.00").compareTo(fee));

                Rule 2 - accessing the class under test. Never reference its private or package-private
                members (e.g. MyCalculator.SOME_PRIVATE_CONSTANT); private fields are inaccessible even from
                the same package and this fails to compile, so inline the literal value instead. Never call
                Mockito's when(...) or verify(...) on the class under test itself (the object annotated
                @InjectMocks or constructed directly) - that throws MissingMethodInvocationException at
                runtime; only stub methods on objects annotated @Mock. Only mock a collaborator the class
                actually depends on via a constructor or field; do not add @Mock/@InjectMocks for a class
                with no dependencies.
                """;
    }

    public String securitySystemPrompt() {
        return """
                You are an application security test engineer (OWASP-focused). Given a description of an
                API endpoint or feature, you generate a JUnit 5 (+ REST Assured if HTTP-based) test class
                covering realistic security checks such as: SQL injection attempts, JWT expiration /
                tampering, broken authentication, CSRF, XSS payloads in inputs, missing/invalid auth
                headers, and rate-limiting behavior. Keep payloads illustrative and non-destructive
                (this is for testing the team's own staging environment).
                """ + COMMON_RULES;
    }

    public String securityUserPrompt(String context) {
        return "Endpoint / feature description:\n" + context + """

                Produce a single JUnit 5 test class named GeneratedSecurityTest.

                Important correctness rule: every attack-simulation test must use a real, realistic
                payload string, never a literal placeholder that just names the attack. For example, for
                SQL injection use something like "' OR '1'='1" or "1; DROP TABLE orders;--", and for XSS
                use something like "<script>alert(1)</script>" or "<img src=x onerror=alert(1)>" as the
                actual value sent in the request. A test that sends the literal string "sql_injection_payload"
                or "xss_payload" tests nothing and must not be produced.

                Important correctness rule: match each payload to where the input is actually used or
                rendered, rather than reaching for a generic template. A bare <script> tag does not execute
                inside an attribute, so if a value lands in an HTML attribute such as <img src="...">, test
                javascript:alert(1) and an attribute-breakout like x" onerror="alert(1); if it is
                interpolated into SQL, test quote-breaking input; if it is used as a redirect target, test
                an open-redirect to an external host; if it is rendered as page text, then a <script> tag is
                the right payload. State the sink you are targeting in a comment above each attack test.

                Important correctness rule: for any test verifying a state-dependent or threshold-dependent
                response, create that state first rather than asserting it on the first request. Rate
                limiting is the common case: the first N requests succeed and only later ones return 429, so
                send the requests in a loop WITHOUT asserting, then assert 429 only on the request that
                crosses the threshold. Asserting 429 (or any other "already happened" status) inside the
                whole loop, including its first iteration, is wrong and will fail against a correct server.
                """;
    }

    public String chatSystemPrompt() {
        return """
                You are TestGen AI's assistant. The user will give you context (requirements, API specs,
                or code) and ask a question about test coverage, missing edge cases, or testing strategy.
                Answer concisely and concretely in plain text (no code unless the user's question implies
                they want a snippet). If asked "which APIs/requirements lack tests" and no test data was
                provided, say so and explain what info you'd need.
                """;
    }

    public String chatUserPrompt(String context, String question) {
        return "Context:\n" + (context == null || context.isBlank() ? "(none provided)" : context) +
                "\n\nQuestion:\n" + question;
    }
}
