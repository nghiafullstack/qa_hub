package vn.qahub.api.testrun;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.qahub.api.common.NotFoundException;
import vn.qahub.api.project.ProjectService;

/** Dashboard đọc lịch sử chạy test — xác thực JWT (xem {@code SecurityConfig.dashboardChain}). */
@RestController
@RequestMapping("/api/v1/projects/{slug}/runs")
@RequiredArgsConstructor
public class TestRunController {

    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectService projectService;

    @GetMapping
    public List<TestRunResponse> listRuns(@PathVariable String slug) {
        Long projectId = projectService.getBySlug(slug).getId();
        return testRunRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(TestRunResponse::from)
                .toList();
    }

    @GetMapping("/{runId}")
    public TestRunDetailResponse getRunDetail(@PathVariable String slug, @PathVariable Long runId) {
        projectService.getBySlug(slug);
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy run: " + runId));
        List<TestCaseResponse> cases = testCaseRepository.findByRunId(runId).stream()
                .map(TestCaseResponse::from)
                .toList();
        return new TestRunDetailResponse(TestRunResponse.from(run), cases);
    }

    public record TestRunDetailResponse(TestRunResponse run, List<TestCaseResponse> testCases) {
    }
}
