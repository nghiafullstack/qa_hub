package vn.rencity.qa.client;

import java.util.Map;

import io.restassured.response.Response;

/**
 * Ví tiền / rút tiền — bc-service, {@code /api/user/community/request_withdrawals/**}.
 *
 * <p>LƯU Ý: chưa có test cho luồng rút tiền THÀNH CÔNG ở đây — cần 1 tài khoản có sẵn số dư
 * ({@code golden_coin}/{@code silver_coin} &gt; 0, nạp qua nghiệp vụ khác như thanh toán hợp
 * đồng/gift coin) VÀ đã liên kết ngân hàng ({@code bank_id} thật) từ trước, không tự tạo được từ
 * 1 tài khoản đăng ký mới hoàn toàn. Xem README mục "Chưa làm / cần dữ liệu có sẵn".
 */
public final class WalletApi {

    private WalletApi() {
    }

    public static Response sendWithdrawalOtp(String userToken) {
        return ApiRequests.post("/api/user/community/request_withdrawals/otp", userToken, Map.of());
    }

    public static Response requestWithdrawal(String userToken, double amountMoney, long bankId, String otp) {
        Map<String, Object> body = Map.of(
                "amount_money", amountMoney,
                "bank_id", bankId,
                "otp", otp,
                "type", 0);
        return ApiRequests.post("/api/user/community/request_withdrawals", userToken, body);
    }
}
