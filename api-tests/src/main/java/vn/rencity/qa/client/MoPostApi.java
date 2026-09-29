package vn.rencity.qa.client;

import java.util.List;
import java.util.Map;

import io.restassured.response.Response;

/**
 * Phòng trọ / bài đăng cho thuê — mo-post-service, qua Gateway {@code /api/user/manage/**}
 * (yêu cầu {@code is_host = true} hoặc {@code is_admin = true} trên token).
 */
public final class MoPostApi {

    /** Mã tỉnh/phường CỐ ĐỊNH, xác nhận hợp lệ trong {@code place.json} dùng chung toàn hệ thống
     *  (Hà Nội / P. Ba Đình) — dùng để test, tránh phải gọi {@code /api/place/**} mỗi lần. */
    public static final long VALID_PROVINCE = 1L;
    public static final long VALID_WARDS = 4L;

    private MoPostApi() {
    }

    public static Response createTower(String hostToken, String towerName) {
        Map<String, Object> body = Map.of(
                "tower_name", towerName,
                "province", VALID_PROVINCE,
                "wards", VALID_WARDS,
                "address_detail", "123 QA Automation Street");
        return ApiRequests.post("/api/user/manage/towers", hostToken, body);
    }

    public static Response createMotel(String hostToken, String motelName, Double money, Long towerId) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("motel_name", motelName);
        body.put("province", VALID_PROVINCE);
        body.put("wards", VALID_WARDS);
        body.put("address_detail", "123 QA Automation Street");
        body.put("images", List.of("https://example.com/qa-fixture.jpg"));
        if (money != null) {
            body.put("money", money);
        }
        if (towerId != null) {
            body.put("tower_id", towerId);
        }
        return ApiRequests.post("/api/user/manage/motels", hostToken, body);
    }

    public static Response createPost(String hostToken, String title, Long motelId) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("title", title);
        body.put("province", VALID_PROVINCE);
        body.put("wards", VALID_WARDS);
        body.put("address_detail", "123 QA Automation Street");
        if (motelId != null) {
            body.put("motel_id", motelId);
        }
        return ApiRequests.post("/api/user/manage/mo_posts", hostToken, body);
    }
}
