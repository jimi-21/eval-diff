package dev.robotwin.evaldiff;

import java.math.BigDecimal;

public record SceneComparison(
        int episode,
        int seed,
        boolean baselineSuccess,
        boolean candidateSuccess,
        int baselineSteps,
        int candidateSteps,
        int stepsDelta,
        BigDecimal baselineJitter,
        BigDecimal candidateJitter,
        String instruction,
        SceneChange change) {
}
