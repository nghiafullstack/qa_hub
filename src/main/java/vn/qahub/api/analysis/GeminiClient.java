package vn.qahub.api.analysis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import vn.qahub.api.common.BadRequestException;

/**
 * Gọi Gemini (Google AI Studio) qua REST API — port từ
 * {@code vn.rencity.mopost.ai.client.GeminiClient} bên rencity-platform-spring (đã chạy thật
 * trên Production/Dev), rút gọn cho use case "1 lượt hỏi → 1 JSON trả lời có cấu trúc" (không
 * cần đa lượt hội thoại/system instruction dài như chatbot). Header {@code x-goog-api-key} thay
 * vì query param {@code ?key=} — cùng lý do bản gốc: query param bị ghi vào access log proxy.
 */
@Component
@Slf4j
public class GeminiClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private final GeminiProperties properties;
    private final RestTemplate restTemplate;

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
        this.restTemplate = buildRestTemplate(properties);
    }

    private static RestTemplate buildRestTemplate(GeminiProperties properties) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(8);
        connectionManager.setDefaultMaxPerRoute(8);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
                .setResponseTimeout(Timeout.ofMilliseconds(properties.getTimeoutMs()))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.getTimeoutMs()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    /**
     * Gửi 1 prompt, nhận về JSON thuần (bật {@code responseMimeType=application/json} — model
     * không bọc markdown/lời dẫn, tránh lỗi parse JSON bị cắt bởi câu chữ thừa).
     */
    @SuppressWarnings("unchecked")
    public String generateJson(String prompt) {
        if (!properties.isConfigured()) {
            throw new BadRequestException(
                    "Chưa cấu hình GEMINI_API_KEY — không gọi được engine phân tích AI."
                            + " Xem README mục cấu hình Gemini.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", properties.getMaxOutputTokens());
        generationConfig.put("responseMimeType", "application/json");
        if (properties.usesThinkingLevels() && properties.getThinkingLevel() != null
                && !properties.getThinkingLevel().isBlank()) {
            generationConfig.put("thinkingConfig", Map.of("thinkingLevel", properties.getThinkingLevel().strip()));
        }
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.effectiveApiKey());

        String url = properties.getBaseUrl() + "/v1beta/models/" + properties.getModel() + ":generateContent";
        int maxAttempts = Math.max(1, 1 + properties.getTimeoutRetries());
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
                String text = extractText(response.getBody());
                if (text == null || text.isBlank()) {
                    throw new BadRequestException("Gemini trả về rỗng — không phân tích được lần này.");
                }
                return text;
            } catch (BadRequestException ex) {
                throw ex;
            } catch (Exception ex) {
                lastFailure = ex;
                if (attempt < maxAttempts && isReadTimeout(ex)) {
                    log.warn("[QA_HUB][GEMINI] Read timed out lần {}/{} model={} — thử lại",
                            attempt, maxAttempts, properties.getModel());
                    continue;
                }
                log.error("[QA_HUB][GEMINI] gọi thất bại model={} url={}: {}", properties.getModel(), url, ex.toString());
                throw new BadRequestException("Gọi Gemini thất bại: " + ex.getMessage());
            }
        }
        throw new BadRequestException(
                "Gọi Gemini thất bại sau " + maxAttempts + " lần: "
                        + (lastFailure != null ? lastFailure.getMessage() : "unknown"));
    }

    private static boolean isReadTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage();
            if (current instanceof ResourceAccessException
                    || (msg != null && msg.toLowerCase().contains("read timed out"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object candidates = payload.get("candidates");
        if (!(candidates instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> candidate)) {
            return null;
        }
        Object content = candidate.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) {
            return null;
        }
        Object parts = contentMap.get("parts");
        if (!(parts instanceof List<?> partList)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object part : partList) {
            if (part instanceof Map<?, ?> partMap && partMap.get("text") != null) {
                sb.append(partMap.get("text"));
            }
        }
        return sb.toString();
    }
}
