package dev.robotwin.evaldiff;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;

public class ScoreAuditLoader {

    private final ObjectMapper objectMapper;

    public ScoreAuditLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ScoreAudit load(Path path) {
        try {
            ScoreAudit audit = objectMapper.readValue(path.toFile(), ScoreAudit.class);
            if (audit.episodeRecords() == null || audit.episodeRecords().isEmpty()) {
                throw new IllegalArgumentException("episode_records must not be empty: " + path);
            }
            if (audit.score() == null || audit.score().total() == null || audit.score().successRate() == null) {
                throw new IllegalArgumentException("score.total and score.success_rate are required: " + path);
            }
            audit.episodeRecords().forEach(episode -> {
                if (episode.steps() < 1) {
                    throw new IllegalArgumentException("Episode steps must be positive: " + path);
                }
            });
            return audit;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read score audit: " + path, exception);
        }
    }
}
