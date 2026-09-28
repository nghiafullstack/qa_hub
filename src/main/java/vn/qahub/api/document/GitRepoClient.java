package vn.qahub.api.document;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;
import vn.qahub.api.common.BadRequestException;

/**
 * Đọc file từ 1 Git repo công khai (GitHub) qua Contents/Trees API — KHÔNG {@code git clone}, đơn
 * giản hơn cho MVP. Không xử lý repo private cần SSH key; repo private cần PAT thì set
 * {@code qahub.github.token}.
 */
@Component
@Slf4j
public class GitRepoClient {

    private static final Pattern GITHUB_URL_PATTERN =
            Pattern.compile("github\\.com[:/]([^/]+)/([^/.]+)(\\.git)?/?$");

    private final RestClient apiClient;
    private final RestClient rawClient;

    public record RepoRef(String owner, String repo) {
    }

    public record GitFile(String path) {
    }

    public GitRepoClient(@Value("${qahub.github.token:}") String githubToken) {
        RestClient.Builder apiBuilder = RestClient.builder().baseUrl("https://api.github.com");
        if (githubToken != null && !githubToken.isBlank()) {
            apiBuilder.defaultHeader("Authorization", "Bearer " + githubToken);
        }
        this.apiClient = apiBuilder.build();
        this.rawClient = RestClient.builder().baseUrl("https://raw.githubusercontent.com").build();
    }

    public RepoRef parseRepoUrl(String gitRepoUrl) {
        if (gitRepoUrl == null) {
            throw new BadRequestException("Dự án chưa cấu hình git_repo_url");
        }
        Matcher matcher = GITHUB_URL_PATTERN.matcher(gitRepoUrl.trim());
        if (!matcher.find()) {
            throw new BadRequestException(
                    "Chỉ hỗ trợ repo GitHub (dạng https://github.com/owner/repo) ở bản đầu: " + gitRepoUrl);
        }
        return new RepoRef(matcher.group(1), matcher.group(2));
    }

    @SuppressWarnings("unchecked")
    public List<GitFile> listFilesMatching(RepoRef repo, String branch, List<String> globPatterns) {
        Map<String, Object> response;
        try {
            response = apiClient.get()
                    .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", repo.owner(), repo.repo(), branch)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadRequestException(
                    "Không truy cập được repo " + repo.owner() + "/" + repo.repo() + " nhánh " + branch
                            + " — repo private cần set QAHUB_GITHUB_TOKEN (Personal Access Token có quyền đọc repo này),"
                            + " hoặc kiểm tra lại tên nhánh.");
        } catch (HttpClientErrorException e) {
            throw new BadRequestException("GitHub API trả lỗi " + e.getStatusCode() + ": " + e.getMessage());
        }
        if (response == null || !(response.get("tree") instanceof List<?> tree)) {
            return List.of();
        }
        return tree.stream()
                .map(entry -> (Map<String, Object>) entry)
                .filter(entry -> "blob".equals(entry.get("type")))
                .map(entry -> (String) entry.get("path"))
                .filter(path -> globPatterns.stream().anyMatch(glob -> GlobMatcher.matches(glob, path)))
                .filter(path -> !SecretPathDenylist.isDenied(path))
                .map(GitFile::new)
                .toList();
    }

    public byte[] fetchRawContent(RepoRef repo, String branch, String path) {
        if (SecretPathDenylist.isDenied(path)) {
            // Không tin tưởng caller — kiểm tra lại lần 2 ngay trước khi thực sự tải nội dung.
            throw new BadRequestException("Đường dẫn nằm trong danh sách bị chặn (nghi chứa secret): " + path);
        }
        return rawClient.get()
                .uri("/{owner}/{repo}/{branch}/{path}", repo.owner(), repo.repo(), branch, path)
                .retrieve()
                .body(byte[].class);
    }
}
