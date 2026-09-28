package vn.qahub.api.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByAnalysisRunId(Long analysisRunId);
}
