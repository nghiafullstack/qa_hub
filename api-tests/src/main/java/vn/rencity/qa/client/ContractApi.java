package vn.rencity.qa.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import io.restassured.response.Response;

/**
 * Hợp đồng thuê phòng — bc-service. Tạo ở {@code /api/user/manage/contracts} (chủ nhà), xác
 * nhận thanh toán ở {@code /api/user/community/contracts/{id}} (khách thuê — người có SĐT trùng
 * với 1 renter trong {@code list_renter} lúc tạo).
 */
public final class ContractApi {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private ContractApi() {
    }

    /**
     * {@code depositMoney = 0} CỐ Ý — {@code ContractCommunityService.paymentContract} chỉ trừ
     * ví khi {@code deposit_money > 0}, nên renter test (ví luôn = 0 vì mới đăng ký) vẫn xác
     * nhận thanh toán được mà không cần nạp tiền trước, giữ flow test tự chứa hoàn toàn.
     */
    public static Response createContract(
            String hostToken, Long motelId, Long towerId, String renterName, String renterPhone, double moneyPerMonth) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> renter = Map.of(
                "name", renterName,
                "phone_number", renterPhone,
                "email", "qa+" + renterPhone + "@example.com",
                "is_represent", true);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("motel_id", motelId);
        body.put("tower_id", towerId);
        body.put("list_renter", List.of(renter));
        body.put("rent_from", now.format(ISO));
        body.put("rent_to", now.plusMonths(6).format(ISO));
        body.put("pay_start", now.format(ISO));
        body.put("payment_space", 1);
        body.put("money", moneyPerMonth);
        body.put("deposit_money", 0);

        return ApiRequests.post("/api/user/manage/contracts", hostToken, body);
    }

    public static Response confirmPaymentAsRenter(String renterToken, long contractId) {
        return ApiRequests.put("/api/user/community/contracts/" + contractId, renterToken, null);
    }

    public static Response getAsRenter(String renterToken, long contractId) {
        return ApiRequests.get("/api/user/community/contracts/" + contractId, renterToken);
    }
}
