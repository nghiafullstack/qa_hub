package vn.qahub.api.testrun;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Đọc XML report chuẩn Surefire/JUnit ({@code target/surefire-reports/TEST-*.xml}) — 1 file
 * root {@code <testsuite>} tương ứng 1 class test. KHÔNG hỗ trợ {@code @DisplayName} (Surefire
 * XML mặc định chỉ ghi tên method ở attribute {@code name}, không ghi display name) — dùng tên
 * method làm {@code displayName} tạm, cải thiện sau bằng SDK nếu cần giữ nguyên mô tả tiếng Việt.
 */
@Component
public class JUnitXmlReportParser {

    public record ParsedTestCase(
            String className, String methodName, TestCaseStatus status,
            Long durationMs, String failureMessage, String stackTrace) {
    }

    public record ParsedSuite(String suiteName, List<ParsedTestCase> testCases) {
    }

    public ParsedSuite parse(byte[] xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Chặn XXE — file report do CI của 1 dự án gửi lên, không phải nguồn tin cậy tuyệt đối.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            try (InputStream in = new ByteArrayInputStream(xmlContent)) {
                Document doc = builder.parse(in);
                Element root = doc.getDocumentElement();
                String suiteName = root.getAttribute("name");
                List<ParsedTestCase> cases = new ArrayList<>();
                NodeList testcaseNodes = root.getElementsByTagName("testcase");
                for (int i = 0; i < testcaseNodes.getLength(); i++) {
                    cases.add(parseTestCase((Element) testcaseNodes.item(i)));
                }
                return new ParsedSuite(suiteName, cases);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Không đọc được file report JUnit XML: " + e.getMessage(), e);
        }
    }

    private ParsedTestCase parseTestCase(Element testcaseEl) {
        String className = testcaseEl.getAttribute("classname");
        String methodName = testcaseEl.getAttribute("name");
        Long durationMs = parseDurationMs(testcaseEl.getAttribute("time"));

        Element failureEl = firstChild(testcaseEl, "failure");
        Element errorEl = firstChild(testcaseEl, "error");
        Element skippedEl = firstChild(testcaseEl, "skipped");

        TestCaseStatus status;
        String failureMessage = null;
        String stackTrace = null;
        if (failureEl != null) {
            status = TestCaseStatus.FAILED;
            failureMessage = failureEl.getAttribute("message");
            stackTrace = failureEl.getTextContent();
        } else if (errorEl != null) {
            status = TestCaseStatus.ERROR;
            failureMessage = errorEl.getAttribute("message");
            stackTrace = errorEl.getTextContent();
        } else if (skippedEl != null) {
            status = TestCaseStatus.SKIPPED;
        } else {
            status = TestCaseStatus.PASSED;
        }

        return new ParsedTestCase(className, methodName, status, durationMs, failureMessage, stackTrace);
    }

    private Element firstChild(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private Long parseDurationMs(String timeSeconds) {
        if (timeSeconds == null || timeSeconds.isBlank()) {
            return null;
        }
        try {
            return Math.round(Double.parseDouble(timeSeconds) * 1000);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
