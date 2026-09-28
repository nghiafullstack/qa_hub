package vn.qahub.api.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.qahub.api.common.BadRequestException;
import vn.qahub.api.common.NotFoundException;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<Project> listAll() {
        return projectRepository.findAll();
    }

    public Project getBySlug(String slug) {
        return projectRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án: " + slug));
    }

    public Project getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án id=" + id));
    }

    @Transactional
    public Project create(CreateProjectRequest request) {
        if (projectRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Slug đã tồn tại: " + request.getSlug());
        }
        Project project = new Project();
        project.setName(request.getName());
        project.setSlug(request.getSlug());
        project.setGitRepoUrl(request.getGitRepoUrl());
        project.setDescription(request.getDescription());
        return projectRepository.save(project);
    }
}
