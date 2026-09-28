package vn.qahub.api.document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import vn.qahub.api.common.BadRequestException;
import vn.qahub.api.project.Project;
import vn.qahub.api.project.ProjectService;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

    private final DocumentRepository documentRepository;
    private final ProjectService projectService;
    private final GitRepoClient gitRepoClient;
    private final TextExtractionService textExtractionService;
    private final RestClient plainRestClient = RestClient.create();

    @Transactional
    public Document uploadDocument(Long projectId, MultipartFile file) {
        TextExtractionService.ExtractionResult extracted = extract(file);
        Document document = new Document();
        document.setProjectId(projectId);
        document.setSourceType(DocumentSourceType.UPLOAD);
        document.setSourceRef(file.getOriginalFilename());
        document.setTitle(file.getOriginalFilename());
        document.setContentText(extracted.text());
        document.setDocFormat(extracted.docFormat());
        document.setContentHash(sha256(extracted.text()));
        document.setLastSyncedAt(Instant.now());
        return documentRepository.save(document);
    }

    /**
     * Đồng bộ tài liệu từ git_repo_url của dự án theo danh sách glob (vd {@code docs/**&#47;*.md}).
     * Chỉ hỗ trợ GitHub công khai (hoặc private có {@code qahub.github.token}) ở bản đầu — xem
     * {@link GitRepoClient}. Bỏ qua file khớp {@link SecretPathDenylist} dù có khớp glob.
     */
    @Transactional
    public List<Document> syncFromGit(Long projectId, String branch, List<String> globPatterns) {
        Project project = projectService.getById(projectId);
        GitRepoClient.RepoRef repo = gitRepoClient.parseRepoUrl(project.getGitRepoUrl());
        List<GitRepoClient.GitFile> files = gitRepoClient.listFilesMatching(repo, branch, globPatterns);

        return files.stream().map(file -> {
            byte[] content = gitRepoClient.fetchRawContent(repo, branch, file.path());
            TextExtractionService.ExtractionResult extracted;
            try {
                extracted = textExtractionService.extract(file.path(), content);
            } catch (BadRequestException e) {
                log.info("Bỏ qua file không hỗ trợ trích text khi đồng bộ Git: {}", file.path());
                return null;
            }
            String hash = sha256(extracted.text());

            Document document = documentRepository.findByProjectIdAndSourceRef(projectId, file.path())
                    .stream().findFirst().orElseGet(Document::new);
            document.setProjectId(projectId);
            document.setSourceType(DocumentSourceType.GIT_PATH);
            document.setSourceRef(file.path());
            document.setTitle(file.path());
            document.setContentText(extracted.text());
            document.setDocFormat(extracted.docFormat());
            document.setContentHash(hash);
            document.setLastSyncedAt(Instant.now());
            return documentRepository.save(document);
        }).filter(java.util.Objects::nonNull).toList();
    }

    @Transactional
    public Document registerOpenApiSource(Long projectId, String url) {
        String json = plainRestClient.get().uri(url).retrieve().body(String.class);
        if (json == null) {
            throw new BadRequestException("Không tải được OpenAPI JSON từ: " + url);
        }
        Document document = documentRepository.findByProjectIdAndSourceRef(projectId, url)
                .stream().findFirst().orElseGet(Document::new);
        document.setProjectId(projectId);
        document.setSourceType(DocumentSourceType.OPENAPI_URL);
        document.setSourceRef(url);
        document.setTitle("OpenAPI: " + url);
        document.setContentText(json);
        document.setDocFormat("OPENAPI_JSON");
        document.setContentHash(sha256(json));
        document.setLastSyncedAt(Instant.now());
        return documentRepository.save(document);
    }

    public List<Document> list(Long projectId) {
        return documentRepository.findByProjectId(projectId);
    }

    @SneakyThrows
    private TextExtractionService.ExtractionResult extract(MultipartFile file) {
        return textExtractionService.extract(file.getOriginalFilename(), file.getBytes());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
