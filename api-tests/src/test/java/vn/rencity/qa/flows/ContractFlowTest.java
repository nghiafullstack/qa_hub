package vn.rencity.qa.flows;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import vn.rencity.qa.client.ContractApi;
import vn.rencity.qa.fixtures.AuthFixture;
import vn.rencity.qa.fixtures.HostWithListing;
import vn.rencity.qa.fixtures.MoPostFixtures;
import vn.rencity.qa.fixtures.TestUser;

/**
 * Luồng lõi của bc-service: chủ nhà tạo hợp đồng cho 1 phòng đang có → khách thuê xác nhận
 * thanh toán → hợp đồng chuyển COMPLETED. Đây là luồng đã qua nhiều đợt refactor gỡ query chéo
 * service (crm-bc, bc-mopost, bc-aff) gần đây — rủi ro hồi quy cao nếu có PR mới đụng vào.
 */
@DisplayName("Hợp đồng: tạo → khách thuê xác nhận thanh toán → COMPLETED")
class ContractFlowTest {

    /** Khớp {@code ContractStatusCode.COMPLETED} bên bc-service — xem enum trước khi đổi số này. */
    private static final int STATUS_COMPLETED = 2;

    @Test
    @DisplayName("Tạo hợp đồng (chủ nhà) rồi khách thuê xác nhận thanh toán (đặt cọc = 0) → status COMPLETED")
    void createContract_thenRenterConfirmsPayment_becomesCompleted() {
        HostWithListing listing = MoPostFixtures.createHostWithListing();
        TestUser renter = AuthFixture.registerRenterAndLogin();

        Response createResponse = ContractApi.createContract(
                listing.host().accessToken(), listing.motelId(), listing.towerId(),
                renter.name(), renter.phoneNumber(), 2_500_000d);
        assertThat(createResponse.statusCode())
                .as("Tạo hợp đồng thất bại: %s", createResponse.asString())
                .isEqualTo(200);
        long contractId = createResponse.jsonPath().getLong("data.id");
        assertThat(contractId).as("Hợp đồng vừa tạo phải có id").isPositive();
        assertThat(createResponse.jsonPath().getInt("data.motel_id")).isEqualTo(listing.motelId().intValue());

        Response paymentResponse = ContractApi.confirmPaymentAsRenter(renter.accessToken(), contractId);
        assertThat(paymentResponse.statusCode())
                .as("Khách thuê xác nhận thanh toán thất bại: %s", paymentResponse.asString())
                .isEqualTo(200);
        assertThat(paymentResponse.jsonPath().getInt("data.status"))
                .as("Hợp đồng phải chuyển COMPLETED (2) sau khi khách thuê xác nhận thanh toán")
                .isEqualTo(STATUS_COMPLETED);

        Response detailResponse = ContractApi.getAsRenter(renter.accessToken(), contractId);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(detailResponse.jsonPath().getInt("data.status")).isEqualTo(STATUS_COMPLETED);
    }

    @Test
    @DisplayName("Người KHÔNG có tên trong hợp đồng cố xác nhận thanh toán → phải bị từ chối")
    void confirmPayment_byUnrelatedUser_isRejected() {
        HostWithListing listing = MoPostFixtures.createHostWithListing();
        TestUser realRenter = AuthFixture.registerRenterAndLogin();
        TestUser strangerRenter = AuthFixture.registerRenterAndLogin();

        Response createResponse = ContractApi.createContract(
                listing.host().accessToken(), listing.motelId(), listing.towerId(),
                realRenter.name(), realRenter.phoneNumber(), 2_500_000d);
        assertThat(createResponse.statusCode()).isEqualTo(200);
        long contractId = createResponse.jsonPath().getLong("data.id");

        Response paymentByStranger = ContractApi.confirmPaymentAsRenter(strangerRenter.accessToken(), contractId);

        assertThat(paymentByStranger.statusCode())
                .as("Người lạ (SĐT không nằm trong list_renter) xác nhận thanh toán được là lỗ hổng phân quyền")
                .isNotEqualTo(200);
    }
}
