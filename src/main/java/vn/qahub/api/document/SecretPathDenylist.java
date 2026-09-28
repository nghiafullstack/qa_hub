package vn.qahub.api.document;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Chặn KHÔNG BAO GIỜ nạp các file có khả năng chứa secret khi đồng bộ tài liệu từ 1 Git repo —
 * bắt buộc, không tuỳ chọn. Sinh ra trực tiếp từ phát hiện thật khi khảo sát repo Rencity:
 * {@code services/*.env} nằm ngay trong working tree (dù gitignore) chứa connection string MySQL
 * thật. Áp dụng độc lập với glob mà người dùng cấu hình — dù ai đó lỡ thêm {@code services/**}
 * vào danh sách đồng bộ, file khớp denylist vẫn KHÔNG được đọc nội dung, không lưu vào
 * {@code documents}, và chắc chắn không bao giờ được gửi cho AI.
 */
public final class SecretPathDenylist {

    private static final List<Pattern> DENY_PATTERNS = List.of(
            Pattern.compile("(^|/)\\.env($|\\..*)"),
            Pattern.compile("(?i).*\\.pem$"),
            Pattern.compile("(?i).*\\.key$"),
            Pattern.compile("(?i).*secret.*"),
            Pattern.compile("(?i).*credential.*"),
            Pattern.compile("(?i)(^|/)id_rsa($|\\..*)"));

    private SecretPathDenylist() {
    }

    public static boolean isDenied(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        return DENY_PATTERNS.stream().anyMatch(p -> p.matcher(normalized).find());
    }
}
