package com.testgenai.service;

import com.testgenai.dto.GenerateResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestGenerationService {

    private static final Pattern FENCE = Pattern.compile("```(?:java|json|yaml)?\\n?([\\s\\S]*?)```");
    private static final Pattern CLASS_NAME = Pattern.compile("class\\s+([A-Za-z0-9_]+)");
    private static final Pattern PACKAGE_DECL = Pattern.compile("^\\s*package\\s+[\\w.]+;", Pattern.MULTILINE);

    /**
     * Types the AI has been observed to use without importing (e.g. BigDecimal in generated
     * test code). This is a deterministic safety net, not a substitute for good prompting:
     * prompting reduces how often the model reasons incorrectly about a type, this catches
     * the purely mechanical slip of using a type without importing it. Extend this map as
     * new recurring missing-import patterns are observed.
     */
    private static final Map<String, String> COMMON_MISSING_IMPORTS = new LinkedHashMap<>();
    static {
        // Common data types the AI has been observed to use without importing.
        COMMON_MISSING_IMPORTS.put("BigDecimal", "java.math.BigDecimal");
        COMMON_MISSING_IMPORTS.put("BigInteger", "java.math.BigInteger");
        COMMON_MISSING_IMPORTS.put("LocalDate", "java.time.LocalDate");
        COMMON_MISSING_IMPORTS.put("LocalDateTime", "java.time.LocalDateTime");
        COMMON_MISSING_IMPORTS.put("LocalTime", "java.time.LocalTime");
        COMMON_MISSING_IMPORTS.put("Duration", "java.time.Duration");
        COMMON_MISSING_IMPORTS.put("Instant", "java.time.Instant");
        COMMON_MISSING_IMPORTS.put("List", "java.util.List");
        COMMON_MISSING_IMPORTS.put("ArrayList", "java.util.ArrayList");
        COMMON_MISSING_IMPORTS.put("Map", "java.util.Map");
        COMMON_MISSING_IMPORTS.put("HashMap", "java.util.HashMap");
        COMMON_MISSING_IMPORTS.put("Set", "java.util.Set");
        COMMON_MISSING_IMPORTS.put("HashSet", "java.util.HashSet");
        COMMON_MISSING_IMPORTS.put("Optional", "java.util.Optional");
        COMMON_MISSING_IMPORTS.put("UUID", "java.util.UUID");
        COMMON_MISSING_IMPORTS.put("Arrays", "java.util.Arrays");
        COMMON_MISSING_IMPORTS.put("Collections", "java.util.Collections");

        // JUnit 5 / Mockito annotations and helpers observed missing in testing
        // (e.g. @DisplayName used without importing org.junit.jupiter.api.DisplayName).
        COMMON_MISSING_IMPORTS.put("Test", "org.junit.jupiter.api.Test");
        COMMON_MISSING_IMPORTS.put("DisplayName", "org.junit.jupiter.api.DisplayName");
        COMMON_MISSING_IMPORTS.put("BeforeEach", "org.junit.jupiter.api.BeforeEach");
        COMMON_MISSING_IMPORTS.put("BeforeAll", "org.junit.jupiter.api.BeforeAll");
        COMMON_MISSING_IMPORTS.put("AfterEach", "org.junit.jupiter.api.AfterEach");
        COMMON_MISSING_IMPORTS.put("AfterAll", "org.junit.jupiter.api.AfterAll");
        COMMON_MISSING_IMPORTS.put("ExtendWith", "org.junit.jupiter.api.extension.ExtendWith");
        COMMON_MISSING_IMPORTS.put("ParameterizedTest", "org.junit.jupiter.params.ParameterizedTest");
        COMMON_MISSING_IMPORTS.put("ValueSource", "org.junit.jupiter.params.provider.ValueSource");
        COMMON_MISSING_IMPORTS.put("MethodSource", "org.junit.jupiter.params.provider.MethodSource");
        COMMON_MISSING_IMPORTS.put("CsvSource", "org.junit.jupiter.params.provider.CsvSource");
        COMMON_MISSING_IMPORTS.put("Arguments", "org.junit.jupiter.params.provider.Arguments");
        COMMON_MISSING_IMPORTS.put("Mock", "org.mockito.Mock");
        COMMON_MISSING_IMPORTS.put("InjectMocks", "org.mockito.InjectMocks");
        COMMON_MISSING_IMPORTS.put("Spy", "org.mockito.Spy");
        COMMON_MISSING_IMPORTS.put("MockitoExtension", "org.mockito.junit.jupiter.MockitoExtension");
    }

    private final GroqAiService aiService;
    private final PromptBuilder prompts;

    public TestGenerationService(GroqAiService aiService, PromptBuilder prompts) {
        this.aiService = aiService;
        this.prompts = prompts;
    }

    public GenerateResponse fromRequirement(String requirementText) {
        return run(prompts.requirementSystemPrompt(), prompts.requirementUserPrompt(requirementText),
                "GeneratedRequirementTest.java");
    }

    public GenerateResponse fromOpenApiSpec(String specText) {
        return run(prompts.openApiSystemPrompt(), prompts.openApiUserPrompt(specText),
                "GeneratedApiTest.java");
    }

    public GenerateResponse fromCode(String codeText) {
        return run(prompts.codeSystemPrompt(), prompts.codeUserPrompt(codeText),
                "GeneratedCodeTest.java");
    }

    public GenerateResponse fromSecurityContext(String context) {
        return run(prompts.securitySystemPrompt(), prompts.securityUserPrompt(context),
                "GeneratedSecurityTest.java");
    }

    public GenerateResponse chat(String context, String question) {
        try {
            String answer = aiService.complete(prompts.chatSystemPrompt(), prompts.chatUserPrompt(context, question));
            return GenerateResponse.ok(answer.trim(), null);
        } catch (Exception e) {
            return GenerateResponse.fail(e.getMessage());
        }
    }

    private GenerateResponse run(String systemPrompt, String userPrompt, String defaultFileName) {
        try {
            String raw = aiService.complete(systemPrompt, userPrompt);
            String cleaned = stripFences(raw).trim();
            cleaned = ensureTestAnnotations(cleaned);
            cleaned = ensureImports(cleaned);
            String fileName = deriveFileName(cleaned, defaultFileName);
            return GenerateResponse.ok(cleaned, fileName);
        } catch (Exception e) {
            return GenerateResponse.fail(e.getMessage());
        }
    }

    private String stripFences(String text) {
        Matcher m = FENCE.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return text;
    }

    /**
     * Detects a method annotated with @DisplayName but missing @Test (or another JUnit test
     * annotation) and inserts @Test. This was observed in generated output and is the most
     * dangerous slip the model makes: the file still compiles and the suite reports all green,
     * but the unannotated method silently never runs, so nothing surfaces the missing coverage.
     * Being purely mechanical, it's fixed here deterministically rather than via prompting.
     */
    private String ensureTestAnnotations(String code) {
        String[] lines = code.split("\n", -1);
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            out.append(line);
            if (i < lines.length - 1) {
                out.append("\n");
            }

            String trimmed = line.trim();
            if (!trimmed.startsWith("@DisplayName")) {
                continue;
            }

            // A @DisplayName on the class itself is legitimate without @Test - only look at
            // members whose following lines declare a method, and check the surrounding
            // annotations for an existing test annotation.
            boolean hasTestAnnotation = false;
            int methodLine = -1;

            for (int j = i + 1; j < lines.length && j <= i + 6; j++) {
                String next = lines[j].trim();
                if (next.isEmpty()) {
                    continue;
                }
                if (next.startsWith("@")) {
                    if (isTestAnnotation(next)) {
                        hasTestAnnotation = true;
                    }
                    continue;
                }
                if (next.contains("(") && !next.startsWith("//") && !next.startsWith("*")
                        && !next.contains("class ")) {
                    methodLine = j;
                }
                break;
            }

            // Also look backwards: @Test may sit above @DisplayName.
            for (int j = i - 1; j >= 0 && j >= i - 6; j--) {
                String prev = lines[j].trim();
                if (prev.isEmpty()) {
                    continue;
                }
                if (prev.startsWith("@")) {
                    if (isTestAnnotation(prev)) {
                        hasTestAnnotation = true;
                    }
                    continue;
                }
                break;
            }

            if (methodLine > 0 && !hasTestAnnotation) {
                String indent = line.substring(0, line.indexOf('@'));
                out.append(indent).append("@Test\n");
            }
        }

        return out.toString();
    }

    private boolean isTestAnnotation(String annotationLine) {
        return annotationLine.startsWith("@Test")
                || annotationLine.startsWith("@ParameterizedTest")
                || annotationLine.startsWith("@RepeatedTest")
                || annotationLine.startsWith("@TestFactory")
                || annotationLine.startsWith("@TestTemplate")
                || annotationLine.startsWith("@BeforeEach")
                || annotationLine.startsWith("@BeforeAll")
                || annotationLine.startsWith("@AfterEach")
                || annotationLine.startsWith("@AfterAll");
    }

    /**
     * Scans the generated code for usages of commonly-forgotten types (BigDecimal, LocalDate,
     * etc.) and inserts any missing import statements. This is a mechanical safety net for a
     * generation slip observed in testing (the model using java.math.BigDecimal without
     * importing it), independent of how well the prompt is written.
     */
    private String ensureImports(String code) {
        StringBuilder missingImports = new StringBuilder();

        for (Map.Entry<String, String> entry : COMMON_MISSING_IMPORTS.entrySet()) {
            String simpleName = entry.getKey();
            String fqcn = entry.getValue();

            boolean usesType = Pattern.compile("\\b" + Pattern.quote(simpleName) + "\\b").matcher(code).find();
            if (!usesType) {
                continue;
            }

            boolean alreadyImported = code.contains("import " + fqcn + ";")
                    || Pattern.compile("import\\s+" + fqcn.substring(0, fqcn.lastIndexOf('.')) + "\\.\\*;").matcher(code).find();
            if (alreadyImported) {
                continue;
            }

            missingImports.append("import ").append(fqcn).append(";\n");
        }

        if (missingImports.isEmpty()) {
            return code;
        }

        Matcher packageMatcher = PACKAGE_DECL.matcher(code);
        if (packageMatcher.find()) {
            int insertAt = packageMatcher.end();
            String after = code.substring(insertAt).replaceFirst("^\\n+", "");
            return code.substring(0, insertAt) + "\n\n" + missingImports.toString().trim() + "\n\n" + after;
        }

        // No package declaration found - insert at the very top.
        return missingImports + "\n" + code;
    }

    private String deriveFileName(String code, String defaultFileName) {
        Matcher m = CLASS_NAME.matcher(code);
        if (m.find()) {
            return m.group(1) + ".java";
        }
        return defaultFileName;
    }
}
