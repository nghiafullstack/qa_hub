package vn.qahub.api.testrun;

/**
 * Rút {@code business_flow_tag} từ tên class test theo quy ước, KHÔNG cần dự án sửa code test
 * (mọi framework JUnit đặt tên class theo PascalCase là dùng được ngay). Ví dụ:
 * {@code AuthFlowTest} → {@code auth}, {@code CrmReservationFlowTest} → {@code crm_reservation}.
 *
 * <p>Đây là bản đơn giản cho MVP (gom theo CLASS, không phải theo từng flow nghiệp vụ nhỏ hơn
 * trong 1 class) — chính xác hơn cần dự án dùng SDK khai báo tag tường minh (xem plan mục
 * "KHÔNG làm ở bản đầu").
 */
public final class BusinessFlowTagExtractor {

    private static final String[] SUFFIXES_TO_STRIP = {"FlowTest", "Test", "Tests", "IT", "Spec"};

    private BusinessFlowTagExtractor() {
    }

    public static String fromClassName(String simpleClassName) {
        if (simpleClassName == null || simpleClassName.isBlank()) {
            return null;
        }
        String name = simpleClassName;
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            name = name.substring(lastDot + 1);
        }
        for (String suffix : SUFFIXES_TO_STRIP) {
            if (name.endsWith(suffix) && name.length() > suffix.length()) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }
        return camelToSnakeCase(name);
    }

    private static String camelToSnakeCase(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
