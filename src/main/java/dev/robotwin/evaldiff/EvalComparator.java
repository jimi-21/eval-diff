package dev.robotwin.evaldiff;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EvalComparator {

    public ComparisonReport compare(ScoreAudit baseline, ScoreAudit candidate) {
        validateScenes(baseline, candidate);
        Map<Integer, ScoreAudit.Episode> candidateByEpisode = byEpisode(candidate);
        List<SceneComparison> scenes = new ArrayList<>();
        for (ScoreAudit.Episode baselineEpisode : baseline.episodeRecords()) {
            ScoreAudit.Episode candidateEpisode = candidateByEpisode.get(baselineEpisode.episode());
            scenes.add(compareScene(baselineEpisode, candidateEpisode));
        }
        List<MetricDelta> metrics = metrics(baseline.score(), candidate.score());
        int improvements = count(scenes, SceneChange.IMPROVEMENT);
        int regressions = count(scenes, SceneChange.REGRESSION);
        int faster = count(scenes, SceneChange.FASTER);
        int slower = count(scenes, SceneChange.SLOWER);
        String verdict = verdict(scenes, metrics);
        return new ComparisonReport(scenes.size(), improvements, regressions, faster, slower, verdict,
                List.copyOf(metrics), List.copyOf(scenes));
    }

    private SceneComparison compareScene(ScoreAudit.Episode baseline, ScoreAudit.Episode candidate) {
        SceneChange change;
        if (baseline.success() && !candidate.success()) {
            change = SceneChange.REGRESSION;
        } else if (!baseline.success() && candidate.success()) {
            change = SceneChange.IMPROVEMENT;
        } else if (!baseline.success()) {
            change = SceneChange.UNCHANGED_FAILURE;
        } else if (candidate.steps() < baseline.steps()) {
            change = SceneChange.FASTER;
        } else if (candidate.steps() > baseline.steps()) {
            change = SceneChange.SLOWER;
        } else {
            change = SceneChange.UNCHANGED_SUCCESS;
        }
        return new SceneComparison(baseline.episode(), baseline.seed(), baseline.success(), candidate.success(),
                baseline.steps(), candidate.steps(), candidate.steps() - baseline.steps(), baseline.jitter(),
                candidate.jitter(), baseline.instruction(), change);
    }

    private List<MetricDelta> metrics(ScoreAudit.Score baseline, ScoreAudit.Score candidate) {
        List<MetricDelta> metrics = new ArrayList<>();
        addMetric(metrics, "totalScore", baseline.total(), candidate.total());
        addMetric(metrics, "successRate", baseline.successRate(), candidate.successRate());
        addMetric(metrics, "meanSuccessSteps", baseline.meanSuccessSteps(), candidate.meanSuccessSteps());
        addMetric(metrics, "meanJitter", baseline.meanJitter(), candidate.meanJitter());
        return metrics;
    }

    private void addMetric(List<MetricDelta> metrics, String name, BigDecimal baseline, BigDecimal candidate) {
        if (baseline != null && candidate != null) {
            metrics.add(new MetricDelta(name, baseline, candidate, candidate.subtract(baseline)));
        }
    }

    private String verdict(List<SceneComparison> scenes, List<MetricDelta> metrics) {
        if (scenes.stream().anyMatch(scene -> scene.change() == SceneChange.REGRESSION)
                || metricIsNegative(metrics, "totalScore") || metricIsNegative(metrics, "successRate")) {
            return "REGRESSION";
        }
        if (scenes.stream().anyMatch(scene -> scene.change() == SceneChange.IMPROVEMENT)
                || metricIsPositive(metrics, "totalScore") || metricIsPositive(metrics, "successRate")) {
            return "IMPROVED";
        }
        return "NO_CHANGE";
    }

    private boolean metricIsNegative(List<MetricDelta> metrics, String name) {
        return metrics.stream().anyMatch(metric -> metric.name().equals(name) && metric.delta().signum() < 0);
    }

    private boolean metricIsPositive(List<MetricDelta> metrics, String name) {
        return metrics.stream().anyMatch(metric -> metric.name().equals(name) && metric.delta().signum() > 0);
    }

    private int count(List<SceneComparison> scenes, SceneChange change) {
        return (int) scenes.stream().filter(scene -> scene.change() == change).count();
    }

    private Map<Integer, ScoreAudit.Episode> byEpisode(ScoreAudit audit) {
        Map<Integer, ScoreAudit.Episode> result = new HashMap<>();
        audit.episodeRecords().forEach(episode -> result.put(episode.episode(), episode));
        return result;
    }

    private void validateScenes(ScoreAudit baseline, ScoreAudit candidate) {
        if (baseline.episodeRecords().size() != candidate.episodeRecords().size()) {
            throw new IllegalArgumentException("Episode counts differ between baseline and candidate");
        }
        Set<Integer> baselineIndexes = new HashSet<>();
        Set<Integer> candidateIndexes = new HashSet<>();
        Set<Integer> baselineSeeds = new HashSet<>();
        Set<Integer> candidateSeeds = new HashSet<>();
        baseline.episodeRecords().forEach(episode -> {
            if (!baselineIndexes.add(episode.episode()) || !baselineSeeds.add(episode.seed())) {
                throw new IllegalArgumentException("Baseline contains duplicate episodes or seeds");
            }
        });
        candidate.episodeRecords().forEach(episode -> {
            if (!candidateIndexes.add(episode.episode()) || !candidateSeeds.add(episode.seed())) {
                throw new IllegalArgumentException("Candidate contains duplicate episodes or seeds");
            }
        });
        if (!baselineIndexes.equals(candidateIndexes) || !baselineSeeds.equals(candidateSeeds)) {
            throw new IllegalArgumentException("Baseline and candidate must contain identical episode indexes and seeds");
        }
        Map<Integer, ScoreAudit.Episode> candidateByEpisode = byEpisode(candidate);
        baseline.episodeRecords().forEach(episode -> {
            if (candidateByEpisode.get(episode.episode()).seed() != episode.seed()) {
                throw new IllegalArgumentException("Seed changed at episode " + episode.episode());
            }
        });
    }
}
