package com.whatxe.xlib.progression;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProgressionJsonReferenceDocsTest {
    @Test
    void generatedMarkdownIncludesShippedProgressionSurfaces() {
        String markdown = ProgressionJsonReferenceDocs.renderMarkdown();

        assertTrue(markdown.contains("## Upgrade Requirement JSON"));
        assertTrue(markdown.contains("## Upgrade Node Definitions"));
        assertTrue(markdown.contains("## Upgrade Kill Rule Definitions"));
        assertTrue(markdown.contains("`condition_ref` ids"));
        assertTrue(markdown.contains("Nested parser errors report JSON paths"));
    }

    @Test
    void checkedInReferenceDocCoversGeneratedHeadingsAndKeyFields() throws IOException {
        Path docPath = Path.of("docs", "wiki", "Progression-JSON-Reference.md");
        String actual = normalizeLineEndings(Files.readString(docPath));
        String generated = normalizeLineEndings(ProgressionJsonReferenceDocs.renderMarkdown());

        assertTrue(actual.contains("## Upgrade Requirement JSON"));
        assertTrue(actual.contains("## Upgrade Node Definitions"));
        assertTrue(actual.contains("## Upgrade Kill Rule Definitions"));
        assertTrue(actual.contains("`conditions` | `array<ability_requirement>`"));
        assertTrue(actual.contains("Nested parser errors report JSON paths"));
        assertTrue(generated.contains("## Upgrade Requirement JSON"));
        assertTrue(generated.contains("`conditions` | `array<ability_requirement>`"));
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n");
    }
}
