package vn.qahub.api.analysis;

import java.time.Instant;

public record AnalysisRunResponse(
        Long id, Long testRunId, String status, String modelUsed, String errorMessage,
        Instant startedAt, Instant finishedAt) {

    public static AnalysisRunResponse from(AnalysisRun run) {
        return new AnalysisRunResponse(
                run.getId(), run.getTestRunId(), run.getStatus().name(), run.getModelUsed(),
                run.getErrorMessage(), run.getStartedAt(), run.getFinishedAt());
    }
}
