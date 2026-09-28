package vn.qahub.api.testrun;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import vn.qahub.api.common.BadRequestException;
import vn.qahub.api.common.UnauthorizedException;
import vn.qahub.api.project.Project;
import vn.qahub.api.project.ProjectService;
import vn.qahub.api.security.ProjectTokenAuthenticationFilter;

/**
 * CI của 1 dự án gọi endpoint này SAU khi chạy test xong để gửi report vào QA Hub — xem
 * {@code rencity-qa-automation/.github/workflows/api-tests.yml} cho ví dụ tích hợp thật.
 */
@RestController
@RequestMapping("/api/v1/ingest/{slug}")
@RequiredArgsConstructor
public class IngestController {

    private final TestRunIngestionService ingestionService;
    private final ProjectService projectService;

    @PostMapping("/runs")
    public IngestRunResponse ingestRun(
            @PathVariable String slug,
            @RequestParam(required = false) String baseUrlTested,
            @RequestParam(required = false) String gitCommitSha,
            @RequestParam(required = false, defaultValue = "CI") String source,
            @RequestParam("report") List<MultipartFile> reports,
            HttpServletRequest request) {
        Project project = projectService.getBySlug(slug);
        Long authenticatedProjectId = (Long) request.getAttribute(
                ProjectTokenAuthenticationFilter.PROJECT_ID_ATTRIBUTE);
        // Token luôn gắn với 1 project cố định (ApiTokenService) — chặn trường hợp dùng token của
        // dự án A để gửi kết quả gắn nhãn dự án B qua path, dù về mặt kỹ thuật token vẫn hợp lệ.
        if (authenticatedProjectId == null || !authenticatedProjectId.equals(project.getId())) {
            throw new UnauthorizedException("Token không thuộc về dự án " + slug);
        }
        if (reports == null || reports.isEmpty()) {
            throw new BadRequestException("Thiếu file report (multipart field 'report')");
        }

        TestRun run = ingestionService.ingest(new TestRunIngestionService.IngestRequest(
                project.getId(), baseUrlTested, gitCommitSha, source, reports));
        return new IngestRunResponse(run.getId(), run.getStatus().name(), run.getTotalTests(),
                run.getTotalFailures(), run.getTotalErrors());
    }

    public record IngestRunResponse(Long runId, String status, int totalTests, int totalFailures, int totalErrors) {
    }
}
