package vn.qahub.api.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, Long> {

    List<AnalysisRun> findByTestRunIdOrderByCreatedAtDesc(Long testRunId);
}
