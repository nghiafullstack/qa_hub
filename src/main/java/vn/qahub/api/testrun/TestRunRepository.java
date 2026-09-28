package vn.qahub.api.testrun;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    List<TestRun> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
