package vn.qahub.api.analysis;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 1 phát hiện — do rule hoặc AI sinh ra, gắn vào 1 {@link AnalysisRun}. */
@Entity
@Table(name = "findings")
@Getter
@Setter
@NoArgsConstructor
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_run_id", nullable = false)
    private Long analysisRunId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FindingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_test_case_id")
    private Long evidenceTestCaseId;

    @Column(name = "evidence_document_id")
    private Long evidenceDocumentId;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingStatus status = FindingStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
