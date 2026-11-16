package dev.robotwin.evaldiff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class EvalDiffApplicationTest {

    @TempDir
    Path tempDir;

    @Test
    void writesMachineReadableAndHtmlReports() throws Exception {
        Path json = tempDir.resolve("out/report.json");
        Path html = tempDir.resolve("out/report.html");
        int exitCode = new EvalDiffApplication().run(new String[]{"examples/baseline.json", "examples/candidate.json",
                "--json", json.toString(), "--html", html.toString()});

        assertEquals(1, exitCode);
        JsonNode report = new ObjectMapper().readTree(json.toFile());
        assertEquals("REGRESSION", report.get("verdict").asText());
        assertEquals(5, report.get("scenes").size());
        String page = Files.readString(html);
        assertTrue(page.contains("Evaluation comparison"));
        assertTrue(page.contains("REGRESSION"));
        assertTrue(page.contains("Local experiment data only"));
    }

    @Test
    void rejectsMismatchedScenePlans() {
        int exitCode = new EvalDiffApplication().run(new String[]{"examples/baseline.json", "examples/baseline.json",
                "extra.json"});
        assertEquals(2, exitCode);
    }

    @Test
    void rejectsReportOverwritingInput() throws Exception {
        Path baseline = Files.copy(Path.of("examples/baseline.json"), tempDir.resolve("baseline.json"));
        byte[] original = Files.readAllBytes(baseline);

        int exitCode = new EvalDiffApplication().run(new String[]{baseline.toString(), "examples/candidate.json",
                "--json", baseline.toString()});

        assertEquals(2, exitCode);
        assertArrayEquals(original, Files.readAllBytes(baseline));
    }

    @Test
    void rejectsIdenticalReportPaths() {
        Path output = tempDir.resolve("comparison.out");

        int exitCode = new EvalDiffApplication().run(new String[]{"examples/baseline.json", "examples/candidate.json",
                "--json", output.toString(), "--html", output.toString()});

        assertEquals(2, exitCode);
        assertFalse(Files.exists(output));
    }

    @Test
    void printsUsage() {
        assertEquals(0, new EvalDiffApplication().run(new String[]{"--help"}));
    }
}
