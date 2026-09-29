package vn.rencity.qa.flows;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import vn.rencity.qa.client.WalletApi;
import vn.rencity.qa.fixtures.AuthFixture;
import vn.rencity.qa.fixtures.TestUser;

/**
 * Ví tiền / rút tiền. CHỈ test được phần "gửi OTP" và các trường hợp phải bị từ chối bằng tài
 * khoản mới đăng ký (số dư = 0, chưa liên kết ngân hàng) — luồng rút tiền THÀNH CÔNG cần tài
 * khoản có sẵn số dư + bank_id thật, chưa tự dựng được từ đầu (xem README).
 */
@DisplayName("Ví tiền: gửi OTP rút tiền + các trường hợp phải bị chặn")
class WalletWithdrawalFlowTest {

    @Test
    @DisplayName("Gửi OTP rút tiền cho tài khoản hợp lệ → thành công")
    void sendWithdrawalOtp_succeeds() {
        TestUser user = AuthFixture.registerRenterAndLogin();

        Response response = WalletApi.sendWithdrawalOtp(user.accessToken());

        assertThat(response.statusCode())
                .as("Gửi OTP rút tiền thất bại: %s", response.asString())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Rút tiền khi ví = 0 và chưa liên kết ngân hàng nào → phải bị từ chối")
    void requestWithdrawal_withNoBalanceAndNoBank_isRejected() {
        TestUser user = AuthFixture.registerRenterAndLogin();
        Response otpResponse = WalletApi.sendWithdrawalOtp(user.accessToken());
        assertThat(otpResponse.statusCode()).isEqualTo(200);
        String otp = otpResponse.jsonPath().getString("data.otp_dev");
        assertThat(otp)
                .as("data.otp_dev chỉ có ở môi trường non-PROD — nếu null, có thể đang chạy nhắm PROD")
                .isNotBlank();

        Response withdrawResponse = WalletApi.requestWithdrawal(user.accessToken(), 1_000_000d, 1L, otp);

        assertThat(withdrawResponse.statusCode())
                .as("Tài khoản mới toanh (0 đồng, chưa liên kết ngân hàng) mà rút tiền được là bug"
                        + " nghiêm trọng — có thể mất tiền thật. Body: %s", withdrawResponse.asString())
                .isNotEqualTo(200);
    }

    @Test
    @DisplayName("Không gửi OTP trước mà rút tiền thẳng với OTP bịa → phải bị từ chối")
    void requestWithdrawal_withFakeOtp_isRejected() {
        // LƯU Ý: ở môi trường KHÔNG PHẢI PROD, WithdrawalsCommunityService CỐ Ý bỏ qua bước so
        // khớp OTP (xem code) — nên nếu test này fail vì trả 200 ở môi trường non-prod, đừng vội
        // kết luận có lỗ hổng OTP; nó vẫn phải bị chặn bởi bước số dư/ngân hàng phía sau (test
        // requestWithdrawal_withNoBalanceAndNoBank_isRejected ở trên) — 2 test cùng khẳng định
        // 1 điều: tài khoản 0 đồng/chưa liên kết ngân hàng không rút được tiền, bất kể qua đường nào.
        TestUser user = AuthFixture.registerRenterAndLogin();

        Response withdrawResponse = WalletApi.requestWithdrawal(user.accessToken(), 1_000_000d, 1L, "000000");

        assertThat(withdrawResponse.statusCode())
                .as("Tài khoản 0 đồng/chưa liên kết ngân hàng rút tiền được (dù OTP bịa) là bug nghiêm trọng")
                .isNotEqualTo(200);
    }
}
