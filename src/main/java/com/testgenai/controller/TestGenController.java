package com.testgenai.controller;

import com.testgenai.dto.GenerateRequest;
import com.testgenai.dto.GenerateResponse;
import com.testgenai.service.GroqAiService;
import com.testgenai.service.TestGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestGenController {

    private final TestGenerationService generationService;
    private final GroqAiService aiService;

    public TestGenController(TestGenerationService generationService, GroqAiService aiService) {
        this.generationService = generationService;
        this.aiService = aiService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "up",
                "aiConfigured", aiService.isConfigured()
        );
    }

    @PostMapping("/generate/requirements")
    public ResponseEntity<GenerateResponse> fromRequirements(@Valid @RequestBody GenerateRequest request) {
        return respond(generationService.fromRequirement(request.getInput()));
    }

    @PostMapping("/generate/openapi")
    public ResponseEntity<GenerateResponse> fromOpenApi(@Valid @RequestBody GenerateRequest request) {
        return respond(generationService.fromOpenApiSpec(request.getInput()));
    }

    @PostMapping("/generate/code")
    public ResponseEntity<GenerateResponse> fromCode(@Valid @RequestBody GenerateRequest request) {
        return respond(generationService.fromCode(request.getInput()));
    }

    @PostMapping("/generate/security")
    public ResponseEntity<GenerateResponse> fromSecurity(@Valid @RequestBody GenerateRequest request) {
        return respond(generationService.fromSecurityContext(request.getInput()));
    }

    @PostMapping("/chat")
    public ResponseEntity<GenerateResponse> chat(@RequestBody GenerateRequest request) {
        String question = request.getQuestion() == null || request.getQuestion().isBlank()
                ? "Summarize the test coverage implications of this context."
                : request.getQuestion();
        return respond(generationService.chat(request.getInput(), question));
    }

    private ResponseEntity<GenerateResponse> respond(GenerateResponse response) {
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(502).body(response);
    }
}
