package vn.rencity.qa.config;

/**
 * Cấu hình môi trường cho toàn bộ bộ test. Mặc định chạy nhắm Dev
 * ({@code https://api-dev.renapp.vn}) — đổi bằng {@code -Drencity.base-url=...} khi chạy
 * {@code mvn test}, hoặc set biến môi trường {@code RENCITY_BASE_URL} (ưu tiên biến môi trường
 * nếu có, tiện cho CI không cần sửa lệnh mvn).
 */
public final class TestConfig {

    private TestConfig() {
    }

    public static String baseUrl() {
        String fromEnv = System.getenv("RENCITY_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return stripTrailingSlash(fromEnv);
        }
        String fromProperty = System.getProperty("rencity.base-url", "https://api-dev.renapp.vn");
        return stripTrailingSlash(fromProperty);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
