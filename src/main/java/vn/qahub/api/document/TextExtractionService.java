package vn.qahub.api.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import vn.qahub.api.common.BadRequestException;

/** Trích text từ nhiều định dạng tài liệu khác nhau — dùng chung cho cả 3 nguồn nạp (Git/upload/OpenAPI JSON). */
@Service
@Slf4j
public class TextExtractionService {

    public record ExtractionResult(String text, String docFormat) {
    }

    public ExtractionResult extract(String fileName, byte[] content) {
        String format = detectFormat(fileName);
        try {
            return switch (format) {
                case "PDF" -> new ExtractionResult(extractPdf(content), format);
                case "DOCX" -> new ExtractionResult(extractDocx(content), format);
                case "MD", "TXT", "JSON", "OPENAPI_JSON" ->
                        new ExtractionResult(new String(content, StandardCharsets.UTF_8), format);
                default -> throw new BadRequestException(
                        "Định dạng file không hỗ trợ: " + fileName
                                + " — chỉ nhận .md/.txt/.json/.pdf/.docx");
            };
        } catch (IOException e) {
            log.warn("Không trích được text từ {}: {}", fileName, e.getMessage());
            throw new BadRequestException("Không đọc được nội dung file " + fileName + ": " + e.getMessage());
        }
    }

    private String detectFormat(String fileName) {
        if (fileName == null) {
            return "TXT";
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "PDF";
        }
        if (lower.endsWith(".docx")) {
            return "DOCX";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "MD";
        }
        if (lower.endsWith(".json")) {
            return "JSON";
        }
        if (lower.endsWith(".txt")) {
            return "TXT";
        }
        return "UNKNOWN";
    }

    private String extractPdf(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
