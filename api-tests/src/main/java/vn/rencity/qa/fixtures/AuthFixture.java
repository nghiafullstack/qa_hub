package vn.rencity.qa.fixtures;

import io.restassured.response.Response;
import vn.rencity.qa.client.AuthApi;

/**
 * Chuỗi tạo 1 tài khoản test hoàn chỉnh từ đầu: gửi OTP → đăng ký → đăng nhập → trả về
 * {@link TestUser} kèm {@code accessToken}. Dùng chung cho mọi flow test cần "1 user bất kỳ"
 * (chủ nhà hoặc khách thuê) mà không phụ thuộc tài khoản có sẵn nào trên Dev.
 */
public final class AuthFixture {

    private AuthFixture() {
    }

    public static TestUser registerAndLogin(String namePrefix, boolean isHost) {
        String phone = TestData.randomPhoneNumber();
        String name = TestData.uniqueName(namePrefix);
        String otp = AuthApi.sendOtpAndExtractCode(phone);

        Response registerResponse = AuthApi.register(name, phone, TestData.TEST_PASSWORD, otp, isHost);
        if (registerResponse.statusCode() != 200) {
            throw new IllegalStateException(
                    "Đăng ký thất bại cho SĐT " + phone + " (status=" + registerResponse.statusCode() + "): "
                            + registerResponse.asString());
        }

        Response loginResponse = AuthApi.login(phone, TestData.TEST_PASSWORD);
        if (loginResponse.statusCode() != 200) {
            throw new IllegalStateException(
                    "Đăng nhập thất bại cho SĐT " + phone + " ngay sau khi đăng ký (status="
                            + loginResponse.statusCode() + "): " + loginResponse.asString());
        }
        String accessToken = loginResponse.jsonPath().getString("data.accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "Login trả 200 nhưng thiếu 'data.accessToken': " + loginResponse.asString());
        }

        return new TestUser(phone, name, TestData.TEST_PASSWORD, isHost, accessToken);
    }

    public static TestUser registerHostAndLogin() {
        return registerAndLogin("QA Host", true);
    }

    public static TestUser registerRenterAndLogin() {
        return registerAndLogin("QA Renter", false);
    }
}
