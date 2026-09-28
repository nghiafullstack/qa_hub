package vn.qahub.api.analysis;

import java.time.Instant;

public record FindingResponse(
        Long id, String category, String severity, String title, String description,
        Long evidenceTestCaseId, boolean aiGenerated, String status, Instant createdAt) {

    public static FindingResponse from(Finding finding) {
        return new FindingResponse(
                finding.getId(), finding.getCategory().name(), finding.getSeverity().name(),
                finding.getTitle(), finding.getDescription(), finding.getEvidenceTestCaseId(),
                finding.isAiGenerated(), finding.getStatus().name(), finding.getCreatedAt());
    }
}
