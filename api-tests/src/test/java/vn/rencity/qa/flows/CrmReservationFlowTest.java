package vn.rencity.qa.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import vn.rencity.qa.client.CrmApi;
import vn.rencity.qa.fixtures.AuthFixture;
import vn.rencity.qa.fixtures.HostWithListing;
import vn.rencity.qa.fixtures.MoPostFixtures;
import vn.rencity.qa.fixtures.TestUser;

/**
 * CRM: khách đặt lịch xem phòng cho 1 bài đăng thật (mo-post-service phải xác nhận phòng/bài tồn
 * tại — xem {@code ReservationMotelCommunityService.create} gọi {@code MoPostClient}). Luồng vừa
 * refactor gỡ query chéo crm↔mo-post gần đây, nên rủi ro hồi quy cao.
 */
@DisplayName("CRM: đặt lịch xem phòng cho 1 bài đăng có thật")
class CrmReservationFlowTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Test
    @DisplayName("Đặt lịch xem phòng cho bài đăng thật → tạo thành công và xem lại được đúng dữ liệu")
    void createReservation_forRealListing_succeeds() {
        HostWithListing listing = MoPostFixtures.createHostWithListing();
        TestUser viewer = AuthFixture.registerRenterAndLogin();
        String visitTime = LocalDateTime.now().plusDays(1).format(ISO);

        Response createResponse = CrmApi.createReservation(
                viewer.accessToken(), listing.postId(), listing.motelId(), listing.towerId(),
                viewer.name(), viewer.phoneNumber(), visitTime);

        assertThat(createResponse.statusCode())
                .as("Đặt lịch xem phòng thất bại: %s", createResponse.asString())
                .isEqualTo(200);
        long reservationId = createResponse.jsonPath().getLong("data.id");
        assertThat(reservationId).isPositive();
        assertThat(createResponse.jsonPath().getLong("data.mo_post_id")).isEqualTo(listing.postId());
        assertThat(createResponse.jsonPath().getString("data.phone_number")).isEqualTo(viewer.phoneNumber());

        Response detailResponse = CrmApi.getDetail(viewer.accessToken(), reservationId);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(detailResponse.jsonPath().getLong("data.id")).isEqualTo(reservationId);
    }

    @Test
    @DisplayName("Đặt lịch xem phòng cho mo_post_id KHÔNG tồn tại → phải bị từ chối, không được tạo bừa")
    void createReservation_forNonExistentListing_isRejected() {
        TestUser viewer = AuthFixture.registerRenterAndLogin();
        long nonExistentPostId = 999_999_999L;
        String visitTime = LocalDateTime.now().plusDays(1).format(ISO);

        Response createResponse = CrmApi.createReservation(
                viewer.accessToken(), nonExistentPostId, null, null,
                viewer.name(), viewer.phoneNumber(), visitTime);

        assertThat(createResponse.statusCode())
                .as("mo_post_id không tồn tại mà vẫn tạo được lịch xem phòng là bug — CRM phải xác thực"
                        + " qua mo-post-service trước khi lưu")
                .isNotEqualTo(200);
    }
}
