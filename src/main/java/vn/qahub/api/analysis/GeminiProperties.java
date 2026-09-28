package vn.qahub.api.analysis;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Cấu hình Gemini cho engine phân tích — CỐ Ý cùng quy ước biến môi trường với module AI Chatbot
 * bên {@code rencity-platform-spring} ({@code GEMINI_API_KEY}/{@code GEMINI_MODEL}/...) để dùng
 * lại đúng key/model/ngân sách đã duyệt sẵn, xem {@code application.yml} và
 * {@code vn.rencity.mopost.ai.config.GeminiProperties} (bản gốc, tham khảo request/response shape).
 */
@Component
@ConfigurationProperties(prefix = "qahub.gemini")
@Getter
@Setter
public class GeminiProperties {

    private String apiKey;
    private String model;
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private int timeoutMs = 180_000;
    private String thinkingLevel = "minimal";
    private int timeoutRetries = 1;
    private int maxOutputTokens = 3072;

    public boolean isConfigured() {
        return effectiveApiKey() != null && model != null && !model.isBlank();
    }

    public String effectiveApiKey() {
        return apiKey == null || apiKey.isBlank() ? null : apiKey.strip();
    }

    public boolean usesThinkingLevels() {
        if (model == null) {
            return false;
        }
        String m = model.toLowerCase(Locale.ROOT);
        return m.startsWith("gemini-3") || m.contains("gemini-3.");
    }
}
