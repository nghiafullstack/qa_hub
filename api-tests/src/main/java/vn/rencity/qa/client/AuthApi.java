package vn.rencity.qa.client;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.restassured.response.Response;

/**
 * Đăng ký / đăng nhập / OTP — aff-service, qua Gateway {@code /api/user/**}. Không có prefix
 * {@code /api/user/manage} hay {@code /community} vì đây là API public (aff-public-throttled),
 * giới hạn 5 request/burst-10 ở Gateway — KHÔNG gọi dồn dập trong test (xem {@code sendOtp}).
 */
public final class AuthApi {

    private AuthApi() {
    }

    public static Response sendOtp(String phoneNumber) {
        return ApiRequests.post("/api/user/send_otp", null, Map.of("phone_number", phoneNumber));
    }

    /**
     * Ở môi trường KHÔNG PHẢI PROD, {@code send_otp} không gửi SMS thật — trả thẳng mã OTP lồng
     * trong câu {@code data.content} (vd {@code "... mã xác thực của bạn ... là 123456"}), KHÔNG
     * có field {@code otp} riêng (xem {@code OtpService.sendOtp}/{@code AffProperties.isDevelopment}).
     * Hàm này gửi OTP rồi tự trích luôn mã 6 số cuối câu, dùng cho {@code register}/{@code check_otp}.
     */
    public static String sendOtpAndExtractCode(String phoneNumber) {
        Response response = sendOtp(phoneNumber);
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "send_otp thất bại (status=" + response.statusCode() + "): " + response.asString());
        }
        String content = response.jsonPath().getString("data.content");
        if (content == null) {
            throw new IllegalStateException(
                    "send_otp không trả 'data.content' — có thể đang chạy nhắm PROD (không lộ OTP)"
                            + " hoặc shape response đã đổi. Body: " + response.asString());
        }
        Matcher matcher = Pattern.compile("(\\d{6})\\s*$").matcher(content.trim());
        if (!matcher.find()) {
            throw new IllegalStateException("Không tìm thấy mã OTP 6 số trong content: " + content);
        }
        return matcher.group(1);
    }

    public static Response register(String name, String phoneNumber, String password, String otp, boolean isHost) {
        return ApiRequests.post("/api/user/register", null, Map.of(
                "name", name,
                "phone_number", phoneNumber,
                "password", password,
                "otp", otp,
                "is_host", isHost));
    }

    public static Response login(String phoneNumber, String password) {
        return ApiRequests.post("/api/user/login", null, Map.of(
                "phone_number", phoneNumber,
                "password", password));
    }

    public static Response checkOtp(String phoneNumber, String otp) {
        return ApiRequests.post("/api/user/check_otp", null, Map.of(
                "phone_number", phoneNumber,
                "otp", otp));
    }
}
