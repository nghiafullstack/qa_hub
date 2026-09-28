package vn.qahub.api.testrun;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByRunId(Long runId);

    List<TestCase> findByRunIdAndStatusIn(Long runId, List<TestCaseStatus> statuses);
}
