package vn.rencity.qa.client;

import java.util.Map;

import io.restassured.response.Response;

/**
 * CRM — đặt lịch xem phòng (reservation_motel), crm-service. Dù nằm dưới path
 * {@code /community/} vẫn yêu cầu đăng nhập (token hợp lệ) — KHÔNG phải API ẩn danh.
 */
public final class CrmApi {

    private CrmApi() {
    }

    public static Response createReservation(
            String userToken, Long moPostId, Long motelId, Long towerId,
            String name, String phoneNumber, String time) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("mo_post_id", moPostId);
        body.put("motel_id", motelId);
        body.put("tower_id", towerId);
        body.put("name", name);
        body.put("phone_number", phoneNumber);
        body.put("time", time);
        body.put("number_people", 1);
        return ApiRequests.post("/api/user/community/reservation_motel", userToken, body);
    }

    public static Response getDetail(String userToken, long reservationId) {
        return ApiRequests.get("/api/user/community/reservation_motel/" + reservationId, userToken);
    }
}
