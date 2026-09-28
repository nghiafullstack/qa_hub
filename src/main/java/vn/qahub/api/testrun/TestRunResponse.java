package vn.qahub.api.testrun;

import java.time.Instant;

public record TestRunResponse(
        Long id, Long projectId, String baseUrlTested, String gitCommitSha, String source,
        String status, int totalTests, int totalFailures, int totalErrors, int totalSkipped,
        Instant startedAt, Instant finishedAt) {

    public static TestRunResponse from(TestRun run) {
        return new TestRunResponse(
                run.getId(), run.getProjectId(), run.getBaseUrlTested(), run.getGitCommitSha(), run.getSource(),
                run.getStatus().name(), run.getTotalTests(), run.getTotalFailures(), run.getTotalErrors(),
                run.getTotalSkipped(), run.getStartedAt(), run.getFinishedAt());
    }
}
