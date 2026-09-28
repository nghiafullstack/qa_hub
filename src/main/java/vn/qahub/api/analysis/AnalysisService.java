package vn.qahub.api.analysis;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.qahub.api.common.NotFoundException;
import vn.qahub.api.document.Document;
import vn.qahub.api.document.DocumentIngestionService;
import vn.qahub.api.document.DocumentSourceType;
import vn.qahub.api.testrun.TestCase;
import vn.qahub.api.testrun.TestCaseRepository;
import vn.qahub.api.testrun.TestCaseStatus;
import vn.qahub.api.testrun.TestRun;
import vn.qahub.api.testrun.TestRunRepository;

/**
 * Tầng 2 của engine phân tích (xem plan mục 4) — gửi Gemini: tài liệu dự án (KHÔNG gồm OpenAPI
 * JSON, loại đó dành cho Tầng 1 rule-based sau này khi có SDK capture request/response) + danh
 * sách test FAILED/ERROR trong 1 run → yêu cầu trả JSON có cấu trúc nêu rõ lệch nghiệp vụ.
 *
 * <p>1 lần bấm "Phân tích" = ĐÚNG 1 lời gọi Gemini cho CẢ RUN (không phải 1 lời gọi/test) — giữ
 * chi phí thấp, khớp quyết định "chỉ phân tích khi bấm nút".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private static final int MAX_DOC_CHARS_EACH = 20_000;
    private static final int MAX_FAILURE_CHARS_EACH = 2_000;

    private final AnalysisRunRepository analysisRunRepository;
    private final FindingRepository findingRepository;
    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final DocumentIngestionService documentIngestionService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AnalysisRun analyze(Long projectId, Long testRunId, Long triggeredByUserId) {
        TestRun run = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy run: " + testRunId));

        AnalysisRun analysisRun = new AnalysisRun();
        analysisRun.setProjectId(projectId);
        analysisRun.setTestRunId(testRunId);
        analysisRun.setTriggeredByUserId(triggeredByUserId);
        analysisRun.setStatus(AnalysisRunStatus.RUNNING);
        analysisRun.setStartedAt(Instant.now());
        analysisRunRepository.save(analysisRun);

        try {
            List<TestCase> notableCases = testCaseRepository.findByRunIdAndStatusIn(
                    testRunId, List.of(TestCaseStatus.FAILED, TestCaseStatus.ERROR));
            if (notableCases.isEmpty()) {
                analysisRun.setStatus(AnalysisRunStatus.DONE);
                analysisRun.setFinishedAt(Instant.now());
                analysisRunRepository.save(analysisRun);
                return analysisRun;
            }

            List<Document> docs = documentIngestionService.list(projectId).stream()
                    .filter(d -> d.getSourceType() != DocumentSourceType.OPENAPI_URL)
                    .toList();

            String prompt = buildPrompt(run, notableCases, docs);
            String rawJson = geminiClient.generateJson(prompt);
            List<Finding> findings = parseFindings(rawJson, analysisRun.getId(), notableCases);
            findingRepository.saveAll(findings);

            analysisRun.setStatus(AnalysisRunStatus.DONE);
            analysisRun.setModelUsed(rawJson == null ? null : "gemini");
        } catch (Exception e) {
            log.warn("[ANALYSIS] Phân tích thất bại run={}: {}", testRunId, e.getMessage());
            analysisRun.setStatus(AnalysisRunStatus.ERROR);
            analysisRun.setErrorMessage(e.getMessage());
        }
        analysisRun.setFinishedAt(Instant.now());
        analysisRunRepository.save(analysisRun);
        return analysisRun;
    }

    private String buildPrompt(TestRun run, List<TestCase> notableCases, List<Document> docs) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Bạn là kỹ sư QA senior. Nhiệm vụ: đọc TÀI LIỆU ĐẶC TẢ và KẾT QUẢ TEST THẬT bên dưới,
                chỉ ra những chỗ HÀNH VI THỰC TẾ của hệ thống KHÁC với những gì tài liệu mô tả/kỳ
                vọng — về Ý NGHĨA NGHIỆP VỤ (không phải lỗi cú pháp/kỹ thuật đơn thuần).

                Chỉ báo cáo khi THỰC SỰ có căn cứ từ tài liệu — nếu tài liệu không nói gì liên quan
                tới 1 test, đừng suy đoán, bỏ qua test đó.

                Trả lời DUY NHẤT 1 JSON đúng schema sau, không thêm chữ nào khác:
                {
                  "findings": [
                    {
                      "test_method": "<đúng tên method test bên dưới>",
                      "category": "BUSINESS_LOGIC_DRIFT" | "SCHEMA_MISMATCH" | "BE_FE_MISMATCH",
                      "severity": "INFO" | "WARN" | "CRITICAL",
                      "title": "<tóm tắt 1 câu bằng tiếng Việt>",
                      "description": "<giải thích chi tiết bằng tiếng Việt: tài liệu nói gì, thực tế ra sao>"
                    }
                  ]
                }
                Nếu không tìm thấy lệch nào có căn cứ, trả {"findings": []}.

                """);

        sb.append("=== TÀI LIỆU DỰ ÁN (").append(docs.size()).append(" tài liệu) ===\n");
        for (Document doc : docs) {
            sb.append("--- Tài liệu: ").append(doc.getTitle()).append(" ---\n");
            sb.append(truncate(doc.getContentText(), MAX_DOC_CHARS_EACH)).append("\n\n");
        }

        sb.append("=== KẾT QUẢ TEST THẬT (run #").append(run.getId())
                .append(", nhắm ").append(run.getBaseUrlTested()).append(") ===\n");
        for (TestCase testCase : notableCases) {
            sb.append("- method: ").append(testCase.getMethodName())
                    .append(" | class: ").append(testCase.getClassName())
                    .append(" | status: ").append(testCase.getStatus())
                    .append("\n  lỗi: ").append(truncate(testCase.getFailureMessage(), MAX_FAILURE_CHARS_EACH))
                    .append("\n");
        }
        return sb.toString();
    }

    private List<Finding> parseFindings(String rawJson, Long analysisRunId, List<TestCase> notableCases) {
        Map<String, Long> testCaseIdByMethod = new HashMap<>();
        for (TestCase testCase : notableCases) {
            testCaseIdByMethod.put(testCase.getMethodName(), testCase.getId());
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode findingsNode = root.get("findings");
            if (findingsNode == null || !findingsNode.isArray()) {
                return List.of();
            }
            return java.util.stream.StreamSupport.stream(findingsNode.spliterator(), false)
                    .map(node -> toFinding(node, analysisRunId, testCaseIdByMethod))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("[ANALYSIS] Không parse được JSON Gemini trả về: {} — raw: {}", e.getMessage(),
                    truncate(rawJson, 500));
            return List.of();
        }
    }

    private Finding toFinding(JsonNode node, Long analysisRunId, Map<String, Long> testCaseIdByMethod) {
        String title = textOrNull(node, "title");
        if (title == null || title.isBlank()) {
            return null;
        }
        Finding finding = new Finding();
        finding.setAnalysisRunId(analysisRunId);
        finding.setCategory(enumOrDefault(textOrNull(node, "category"), FindingCategory.BUSINESS_LOGIC_DRIFT));
        finding.setSeverity(enumOrDefault(textOrNull(node, "severity"), FindingSeverity.WARN));
        finding.setTitle(title);
        finding.setDescription(textOrNull(node, "description"));
        finding.setAiGenerated(true);
        finding.setStatus(FindingStatus.OPEN);
        String method = textOrNull(node, "test_method");
        if (method != null) {
            finding.setEvidenceTestCaseId(testCaseIdByMethod.get(method));
        }
        return finding;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private <E extends Enum<E>> E enumOrDefault(String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "(không có nội dung)";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "... (đã cắt bớt)";
    }
}
