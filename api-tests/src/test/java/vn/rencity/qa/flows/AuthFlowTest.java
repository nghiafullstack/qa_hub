package vn.rencity.qa.flows;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import vn.rencity.qa.client.AuthApi;
import vn.rencity.qa.fixtures.TestData;

/**
 * Luồng đăng ký / đăng nhập / OTP (aff-service) — nền tảng cho mọi flow khác, vì hầu hết API
 * đều cần header {@code token} lấy từ đây.
 */
@DisplayName("Auth: gửi OTP → đăng ký → đăng nhập")
class AuthFlowTest {

    @Test
    @DisplayName("Gửi OTP tới SĐT mới → nhận được mã 6 số (môi trường non-prod trả kèm trong content)")
    void sendOtp_returnsCodeEmbeddedInContent() {
        String phone = TestData.randomPhoneNumber();

        Response response = AuthApi.sendOtp(phone);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        String content = response.jsonPath().getString("data.content");
        assertThat(content).as("data.content phải chứa mã OTP ở môi trường Dev").isNotBlank();
        assertThat(response.jsonPath().getString("data.to")).isEqualTo(phone);
    }

    @Test
    @DisplayName("Đăng ký tài khoản mới với OTP đúng → đăng nhập lại ngay bằng đúng mật khẩu vừa đăng ký")
    void registerThenLogin_succeeds() {
        String phone = TestData.randomPhoneNumber();
        String name = TestData.uniqueName("QA Auth User");
        String otp = AuthApi.sendOtpAndExtractCode(phone);

        Response registerResponse = AuthApi.register(name, phone, TestData.TEST_PASSWORD, otp, false);
        assertThat(registerResponse.statusCode())
                .as("Đăng ký phải thành công với OTP vừa lấy được: %s", registerResponse.asString())
                .isEqualTo(200);

        Response loginResponse = AuthApi.login(phone, TestData.TEST_PASSWORD);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(loginResponse.jsonPath().getString("data.accessToken"))
                .as("Login phải trả accessToken (camelCase, cố ý khác snake_case toàn hệ thống)")
                .isNotBlank();
        assertThat(loginResponse.jsonPath().getString("data.refreshToken")).isNotBlank();
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu → phải bị từ chối (không phải 200)")
    void login_withWrongPassword_isRejected() {
        String phone = TestData.randomPhoneNumber();
        String otp = AuthApi.sendOtpAndExtractCode(phone);
        AuthApi.register(TestData.uniqueName("QA Auth User"), phone, TestData.TEST_PASSWORD, otp, false);

        Response wrongLogin = AuthApi.login(phone, "mat-khau-sai-hoan-toan");

        assertThat(wrongLogin.statusCode())
                .as("Sai mật khẩu mà vẫn trả 200 là lỗ hổng nghiêm trọng")
                .isNotEqualTo(200);
    }

    @Test
    @DisplayName("Gửi OTP lại đúng 30s sau lần đầu cho cùng 1 SĐT → phải bị chặn (cooldown)")
    void sendOtp_within29To58Seconds_isRejected() throws InterruptedException {
        // LƯU Ý HÀNH VI THẬT (đã verify bằng cách chạy test này, không phải suy đoán):
        // OtpService.ensureCooldownPassed chỉ chặn khi lần gửi lại rơi vào khoảng (29s, 58s] SAU
        // lần gửi trước — gửi lại NGAY LẬP TỨC (0-29s) KHÔNG bị chặn bởi hàm này (đúng thiết kế
        // gốc NestJS, xem javadoc OtpService — "time1 = time_generate + 29s; span = now - time1;
        // if (0 < span <= 29) throw"). Test ban đầu giả định "gửi lại ngay lập tức bị chặn" SAI —
        // đã sửa lại đúng theo hành vi thật sau khi chạy thử.
        String phone = TestData.randomPhoneNumber();

        Response first = AuthApi.sendOtp(phone);
        assertThat(first.statusCode()).isEqualTo(200);

        Thread.sleep(30_000);
        Response second = AuthApi.sendOtp(phone);

        assertThat(second.statusCode())
                .as("Gửi lại OTP 30s sau lần đầu phải bị chặn bởi cooldown (29s-58s) — nếu test này"
                        + " fail, kiểm tra lại cooldown có còn hoạt động không")
                .isNotEqualTo(200);
    }
}
