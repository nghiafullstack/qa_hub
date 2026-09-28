package vn.qahub.api.project;

import java.time.Instant;

public record ProjectResponse(Long id, String name, String slug, String gitRepoUrl, String description, Instant createdAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(), project.getName(), project.getSlug(),
                project.getGitRepoUrl(), project.getDescription(), project.getCreatedAt());
    }
}
