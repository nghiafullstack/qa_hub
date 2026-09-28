package vn.qahub.api.analysis;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.qahub.api.common.NotFoundException;
import vn.qahub.api.project.ProjectService;
import vn.qahub.api.security.JwtAuthenticationFilter.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/projects/{slug}/runs/{runId}")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisRunRepository analysisRunRepository;
    private final FindingRepository findingRepository;
    private final ProjectService projectService;

    @PostMapping("/analyze")
    public AnalysisRunResponse analyze(
            @PathVariable String slug, @PathVariable Long runId, @AuthenticationPrincipal AuthenticatedUser user) {
        Long projectId = projectService.getBySlug(slug).getId();
        Long userId = user == null ? null : user.userId();
        return AnalysisRunResponse.from(analysisService.analyze(projectId, runId, userId));
    }

    @GetMapping("/analysis")
    public List<AnalysisRunResponse> listAnalysisRuns(@PathVariable String slug, @PathVariable Long runId) {
        projectService.getBySlug(slug);
        return analysisRunRepository.findByTestRunIdOrderByCreatedAtDesc(runId).stream()
                .map(AnalysisRunResponse::from)
                .toList();
    }

    @GetMapping("/analysis/{analysisRunId}/findings")
    public List<FindingResponse> findings(
            @PathVariable String slug, @PathVariable Long runId, @PathVariable Long analysisRunId) {
        projectService.getBySlug(slug);
        return findingRepository.findByAnalysisRunId(analysisRunId).stream().map(FindingResponse::from).toList();
    }

    @PutMapping("/findings/{findingId}/status")
    public FindingResponse updateFindingStatus(
            @PathVariable String slug, @PathVariable Long runId, @PathVariable Long findingId,
            @Valid @RequestBody UpdateFindingStatusRequest request) {
        projectService.getBySlug(slug);
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy finding: " + findingId));
        finding.setStatus(request.getStatus());
        return FindingResponse.from(findingRepository.save(finding));
    }
}
