package dev.robotwin.evaldiff;

import java.util.List;

public record ComparisonReport(
        int sceneCount,
        int improvements,
        int regressions,
        int fasterScenes,
        int slowerScenes,
        String verdict,
        List<MetricDelta> metrics,
        List<SceneComparison> scenes) {
}
