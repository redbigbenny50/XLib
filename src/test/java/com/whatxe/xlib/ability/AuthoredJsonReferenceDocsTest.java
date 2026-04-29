package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AuthoredJsonReferenceDocsTest {
    @Test
    void generatedMarkdownIncludesCoreAuthoredSurfaces() {
        String markdown = AuthoredJsonReferenceDocs.renderMarkdown();

        assertTrue(markdown.contains("## Named Condition Definitions"));
        assertTrue(markdown.contains("## Authored Ability Definitions"));
        assertTrue(markdown.contains("## Profile Definitions"));
        assertTrue(markdown.contains("## Mode Definitions"));
        assertTrue(markdown.contains("`condition_ref` ids"));
    }

    @Test
    void checkedInReferenceDocCoversGeneratedHeadingsAndKeyFields() throws IOException {
        Path docPath = Path.of("docs", "wiki", "Declarative-JSON-Reference.md");
        String actual = normalizeLineEndings(Files.readString(docPath));
        String generated = normalizeLineEndings(AuthoredJsonReferenceDocs.renderMarkdown());

        assertTrue(actual.contains("## Named Condition Definitions"));
        assertTrue(actual.contains("## Authored Ability Definitions"));
        assertTrue(actual.contains("## Profile Definitions"));
        assertTrue(actual.contains("## Mode Definitions"));
        assertTrue(actual.contains("`grant_state_flags` | `array<resource_location>`"));
        assertTrue(generated.contains("## Authored Passive Definitions"));
        assertTrue(generated.contains("`grant_state_flags` | `array<resource_location>`"));
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n");
    }
}
