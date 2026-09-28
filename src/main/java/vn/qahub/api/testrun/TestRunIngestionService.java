package vn.qahub.api.testrun;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
public class TestRunIngestionService {

    private final JUnitXmlReportParser junitXmlReportParser;
    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;

    public record IngestRequest(
            Long projectId, String baseUrlTested, String gitCommitSha, String source,
            List<MultipartFile> junitXmlReports) {
    }

    @Transactional
    public TestRun ingest(IngestRequest request) {
        TestRun run = new TestRun();
        run.setProjectId(request.projectId());
        run.setBaseUrlTested(request.baseUrlTested());
        run.setGitCommitSha(request.gitCommitSha());
        run.setSource(request.source() == null || request.source().isBlank() ? "CI" : request.source());
        run.setRawReportFormat("JUNIT_XML");
        run.setStatus(TestRunStatus.PENDING);
        run.setStartedAt(Instant.now());
        testRunRepository.save(run);

        int totalTests = 0;
        int totalFailures = 0;
        int totalErrors = 0;
        int totalSkipped = 0;

        for (MultipartFile file : request.junitXmlReports()) {
            JUnitXmlReportParser.ParsedSuite suite = parseFile(file);
            String tag = BusinessFlowTagExtractor.fromClassName(suite.suiteName());
            for (JUnitXmlReportParser.ParsedTestCase parsed : suite.testCases()) {
                TestCase testCase = new TestCase();
                testCase.setRunId(run.getId());
                testCase.setClassName(parsed.className());
                testCase.setMethodName(parsed.methodName());
                testCase.setDisplayName(parsed.methodName());
                testCase.setStatus(parsed.status());
                testCase.setDurationMs(parsed.durationMs());
                testCase.setFailureMessage(truncate(parsed.failureMessage(), 60_000));
                testCase.setStackTrace(truncate(parsed.stackTrace(), 500_000));
                testCase.setBusinessFlowTag(tag);
                testCaseRepository.save(testCase);

                totalTests++;
                switch (parsed.status()) {
                    case FAILED -> totalFailures++;
                    case ERROR -> totalErrors++;
                    case SKIPPED -> totalSkipped++;
                    default -> { }
                }
            }
        }

        run.setTotalTests(totalTests);
        run.setTotalFailures(totalFailures);
        run.setTotalErrors(totalErrors);
        run.setTotalSkipped(totalSkipped);
        run.setStatus(totalFailures > 0 || totalErrors > 0 ? TestRunStatus.FAILED : TestRunStatus.PASSED);
        run.setFinishedAt(Instant.now());
        testRunRepository.save(run);
        return run;
    }

    @SneakyThrows
    private JUnitXmlReportParser.ParsedSuite parseFile(MultipartFile file) {
        return junitXmlReportParser.parse(file.getBytes());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "... (đã cắt bớt)";
    }
}
