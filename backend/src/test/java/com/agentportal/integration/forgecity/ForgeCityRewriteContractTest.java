package com.agentportal.integration.forgecity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeCityRewriteContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesExactVersionOneContract() {
        ForgeCityRewriteContract.Request request = parse("""
                {"schemaVersion":1,"appLabel":"ForgeCity","title":"Alert","text":"Bus 42 delayed","maxChars":120}
                """, 500);

        assertEquals("ForgeCity", request.appLabel());
        assertEquals(120, request.maxChars());
    }

    @Test
    void rejectsUnknownMissingAndWrongTypedFields() {
        assertInvalid("""
                {"schemaVersion":1,"appLabel":"A","title":"","text":"x","maxChars":20,"extra":"x"}
                """);
        assertInvalid("""
                {"schemaVersion":1,"appLabel":"A","title":"","maxChars":20}
                """);
        assertInvalid("""
                {"schemaVersion":"1","appLabel":"A","title":"","text":"x","maxChars":20}
                """);
    }

    @Test
    void rejectsEmptyContentAndOversizeRequestedLimit() {
        assertInvalid("""
                {"schemaVersion":1,"appLabel":"A","title":" ","text":" ","maxChars":20}
                """);
        assertInvalid("""
                {"schemaVersion":1,"appLabel":"A","title":"","text":"x","maxChars":501}
                """);
    }

    @Test
    void keyComparisonRequiresConfiguredExactKey() {
        assertTrue(ForgeCityKeyAuthenticator.matches("secret", "secret"));
        assertFalse(ForgeCityKeyAuthenticator.matches("Secret", "secret"));
        assertFalse(ForgeCityKeyAuthenticator.matches(null, "secret"));
        assertFalse(ForgeCityKeyAuthenticator.matches("secret", " "));
    }

    @Test
    void outputValidatorStripsPrefixAndRequiresTamil() {
        assertEquals("பேருந்து 42 தாமதம்",
                ForgeCityTamilRewriteService.validateOutput("TAMIL: பேருந்து 42 தாமதம்", 40));
        assertThrows(ForgeCityTamilRewriteService.RewriteFailedException.class,
                () -> ForgeCityTamilRewriteService.validateOutput("TAMIL: Bus 42 delayed", 40));
        assertThrows(ForgeCityTamilRewriteService.RewriteFailedException.class,
                () -> ForgeCityTamilRewriteService.validateOutput("பேருந்து தாமதம்", 40));
        assertThrows(ForgeCityTamilRewriteService.RewriteFailedException.class,
                () -> ForgeCityTamilRewriteService.validateOutput("TAMIL: {\"tamil\":\"தாமதம்\"}", 40));
    }

    private ForgeCityRewriteContract.Request parse(String json, int maxChars) {
        return ForgeCityRewriteContract.parse(
                mapper,
                json.getBytes(StandardCharsets.UTF_8),
                maxChars
        );
    }

    private void assertInvalid(String json) {
        assertThrows(ForgeCityRewriteContract.InvalidRequestException.class,
                () -> parse(json, 500));
    }
}
