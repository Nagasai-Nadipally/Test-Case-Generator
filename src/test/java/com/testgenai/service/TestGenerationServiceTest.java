package com.testgenai.service;

import com.testgenai.dto.GenerateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class TestGenerationServiceTest {

    private GroqAiService aiService;
    private TestGenerationService service;

    @BeforeEach
    void setUp() {
        aiService = Mockito.mock(GroqAiService.class);
        service = new TestGenerationService(aiService, new PromptBuilder());
    }

    @Test
    @DisplayName("Strips markdown fences and derives class name from AI output")
    void fromRequirement_stripsFencesAndDerivesFileName() {
        String aiOutput = "```java\npackage com.testgenai.generated;\n\nclass TransferServiceTest {\n}\n```";
        when(aiService.complete(anyString(), anyString())).thenReturn(aiOutput);

        GenerateResponse response = service.fromRequirement("User can transfer money if balance is sufficient.");

        assertTrue(response.isSuccess());
        assertFalse(response.getOutput().contains("```"));
        assertEquals("TransferServiceTest.java", response.getSuggestedFileName());
    }

    @Test
    @DisplayName("Returns failure response when the AI provider throws")
    void fromCode_returnsFailureOnException() {
        when(aiService.complete(anyString(), anyString()))
                .thenThrow(new IllegalStateException("Groq API key is not configured."));

        GenerateResponse response = service.fromCode("class Foo {}");

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        assertTrue(response.getError().contains("API key"));
    }

    @Test
    @DisplayName("Falls back to default file name when no class declaration is found")
    void fromSecurityContext_fallsBackToDefaultFileName() {
        when(aiService.complete(anyString(), anyString())).thenReturn("// no class here, just a comment");

        GenerateResponse response = service.fromSecurityContext("some endpoint");

        assertTrue(response.isSuccess());
        assertEquals("GeneratedSecurityTest.java", response.getSuggestedFileName());
    }

    @Test
    @DisplayName("Auto-inserts a missing BigDecimal import that the AI forgot")
    void fromCode_insertsMissingBigDecimalImport() {
        String aiOutput = """
                package com.testgenai.generated;

                import org.junit.jupiter.api.Test;

                class InvoiceServiceTest {
                    void test() {
                        BigDecimal x = new BigDecimal("1.00");
                    }
                }
                """;
        when(aiService.complete(anyString(), anyString())).thenReturn(aiOutput);

        GenerateResponse response = service.fromCode("class InvoiceService {}");

        assertTrue(response.isSuccess());
        assertTrue(response.getOutput().contains("import java.math.BigDecimal;"));
    }

    @Test
    @DisplayName("Does not duplicate an import that is already present")
    void fromCode_doesNotDuplicateExistingImport() {
        String aiOutput = """
                package com.testgenai.generated;

                import java.math.BigDecimal;
                import org.junit.jupiter.api.Test;

                class InvoiceServiceTest {
                    void test() {
                        BigDecimal x = new BigDecimal("1.00");
                    }
                }
                """;
        when(aiService.complete(anyString(), anyString())).thenReturn(aiOutput);

        GenerateResponse response = service.fromCode("class InvoiceService {}");

        assertTrue(response.isSuccess());
        long occurrences = response.getOutput().split("import java\\.math\\.BigDecimal;", -1).length - 1;
        assertEquals(1, occurrences);
    }

    @Test
    @DisplayName("Auto-inserts a missing @DisplayName import (real bug observed: annotation used without import)")
    void fromCode_insertsMissingDisplayNameImport() {
        String aiOutput = """
                package com.testgenai.generated;

                import org.junit.jupiter.api.Test;

                class BookingAvailabilityCheckerTest {
                    @Test
                    @DisplayName("Test isRoomAvailable with no existing bookings")
                    void test() {
                    }
                }
                """;
        when(aiService.complete(anyString(), anyString())).thenReturn(aiOutput);

        GenerateResponse response = service.fromCode("class BookingAvailabilityChecker {}");

        assertTrue(response.isSuccess());
        assertTrue(response.getOutput().contains("import org.junit.jupiter.api.DisplayName;"));
    }

    @Test
    @DisplayName("Adds a missing @Test to a @DisplayName method (silent no-run bug)")
    void fromCode_addsMissingTestAnnotation() {
        String aiOutput = """
                package com.testgenai.generated;

                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.DisplayName;

                class ShippingQuoteServiceTest {

                    @DisplayName("Test quote with valid weight")
                    void testQuoteValid() {
                    }
                }
                """;
        when(aiService.complete(anyString(), anyString())).thenReturn(aiOutput);

        GenerateResponse response = service.fromCode("class ShippingQuoteService {}");

        assertTrue(response.isSuccess());
        assertTrue(response.getOutput().contains("@Test"));
    }

    @Test
    @DisplayName("Does not duplicate @Test when it is already present")
    void fromCode_doesNotDuplicateTestAnnotation() {
        String aiOutput = """
                package com.testgenai.generated;

                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.DisplayName;

                class FooTest {

                    @Test
                    @DisplayName("already annotated")
                    void testThing() {
                    }
                }
                """;
        when(aiService.complete(anyString(), anyString())).thenReturn(aiOutput);

        GenerateResponse response = service.fromCode("class Foo {}");

        assertTrue(response.isSuccess());
        long occurrences = response.getOutput().split("@Test", -1).length - 1;
        assertEquals(1, occurrences);
    }
}
