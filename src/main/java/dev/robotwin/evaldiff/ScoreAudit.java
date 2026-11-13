package dev.robotwin.evaldiff;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreAudit(@JsonAlias("episode_records") List<Episode> episodeRecords, Score score) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Episode(
            int episode,
            int seed,
            boolean success,
            int steps,
            BigDecimal jitter,
            String instruction,
            String video) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Score(
            BigDecimal total,
            @JsonAlias("success_rate") BigDecimal successRate,
            @JsonAlias("mean_success_steps") BigDecimal meanSuccessSteps,
            @JsonAlias("mean_jitter") BigDecimal meanJitter) {
    }
}
