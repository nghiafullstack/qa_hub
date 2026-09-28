package vn.qahub.api.token;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.qahub.api.project.ProjectService;

@RestController
@RequestMapping("/api/v1/projects/{slug}/tokens")
@RequiredArgsConstructor
public class ApiTokenController {

    private final ApiTokenService apiTokenService;
    private final ProjectService projectService;

    @PostMapping
    public IssueTokenResponse issue(@PathVariable String slug, @RequestBody IssueTokenRequest request) {
        Long projectId = projectService.getBySlug(slug).getId();
        var issued = apiTokenService.issueToken(projectId, request.getName());
        return new IssueTokenResponse(issued.entity().getId(), issued.entity().getName(), issued.rawToken());
    }
}
