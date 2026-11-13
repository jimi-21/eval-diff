package dev.robotwin.evaldiff;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvalComparatorTest {

    private final ScoreAuditLoader loader = new ScoreAuditLoader(new ObjectMapper());
    private final EvalComparator comparator = new EvalComparator();

    @Test
    void detectsRealRegressionAndMetricDeltas() {
        ComparisonReport report = comparator.compare(loader.load(Path.of("examples/baseline.json")),
                loader.load(Path.of("examples/candidate.json")));

        assertEquals(5, report.sceneCount());
        assertEquals("REGRESSION", report.verdict());
        assertEquals(0, report.improvements());
        assertEquals(1, report.regressions());
        assertEquals(1, report.fasterScenes());
        assertEquals(1, report.slowerScenes());
        assertEquals(SceneChange.REGRESSION, report.scenes().get(2).change());
        assertEquals(16, report.scenes().get(2).stepsDelta());
        assertMetric(report, "totalScore", "-4.4");
        assertMetric(report, "successRate", "-0.2");
    }

    @Test
    void detectsImprovementWhenComparisonIsReversed() {
        ComparisonReport report = comparator.compare(loader.load(Path.of("examples/candidate.json")),
                loader.load(Path.of("examples/baseline.json")));

        assertEquals("IMPROVED", report.verdict());
        assertEquals(1, report.improvements());
        assertEquals(0, report.regressions());
        assertMetric(report, "totalScore", "4.4");
        assertMetric(report, "successRate", "0.2");
    }

    private void assertMetric(ComparisonReport report, String name, String expected) {
        MetricDelta metric = report.metrics().stream().filter(item -> item.name().equals(name)).findFirst().orElseThrow();
        assertEquals(0, metric.delta().compareTo(new BigDecimal(expected)));
    }
}
