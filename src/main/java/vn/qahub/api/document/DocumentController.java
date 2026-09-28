package vn.qahub.api.document;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import vn.qahub.api.common.NotFoundException;
import vn.qahub.api.project.ProjectService;

@RestController
@RequestMapping("/api/v1/projects/{slug}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;
    private final DocumentRepository documentRepository;
    private final ProjectService projectService;

    @GetMapping
    public List<DocumentResponse> list(@PathVariable String slug) {
        Long projectId = projectService.getBySlug(slug).getId();
        return documentIngestionService.list(projectId).stream().map(DocumentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DocumentDetailResponse detail(@PathVariable String slug, @PathVariable Long id) {
        projectService.getBySlug(slug);
        return documentRepository.findById(id)
                .map(DocumentDetailResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu: " + id));
    }

    @PostMapping("/upload")
    public DocumentResponse upload(@PathVariable String slug, @RequestParam("file") MultipartFile file) {
        Long projectId = projectService.getBySlug(slug).getId();
        return DocumentResponse.from(documentIngestionService.uploadDocument(projectId, file));
    }

    @PostMapping("/sync-git")
    public List<DocumentResponse> syncGit(@PathVariable String slug, @Valid @RequestBody SyncGitRequest request) {
        Long projectId = projectService.getBySlug(slug).getId();
        return documentIngestionService.syncFromGit(projectId, request.getBranch(), request.getGlobs()).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @PostMapping("/openapi")
    public DocumentResponse registerOpenApi(
            @PathVariable String slug, @Valid @RequestBody RegisterOpenApiRequest request) {
        Long projectId = projectService.getBySlug(slug).getId();
        return DocumentResponse.from(documentIngestionService.registerOpenApiSource(projectId, request.getUrl()));
    }
}
