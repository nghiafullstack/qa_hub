package vn.rencity.qa.flows;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import vn.rencity.qa.client.BillApi;
import vn.rencity.qa.fixtures.CompletedContract;
import vn.rencity.qa.fixtures.ContractFixtures;

/**
 * Chỉ số điện nước + hoá đơn: chủ nhà ghi chỉ số điện/nước hàng tháng → tạo hoá đơn cho hợp đồng
 * đã COMPLETED → khách thuê xem và báo đã thanh toán.
 */
@DisplayName("Hoá đơn: ghi chỉ số điện nước → tạo hoá đơn → khách thuê xem/báo đã trả")
class BillingFlowTest {

    @Test
    @DisplayName("Ghi chỉ số điện nước cho hợp đồng đã COMPLETED → thành công")
    void recordUtilityIndex_forCompletedContract_succeeds() {
        CompletedContract contract = ContractFixtures.createCompletedContract();

        Response response = BillApi.recordUtilityIndex(
                contract.listing().host().accessToken(), contract.contractId(), contract.listing().motelId(),
                100, 150, 50, 70);

        assertThat(response.statusCode())
                .as("Ghi chỉ số điện nước thất bại: %s", response.asString())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Tạo hoá đơn cho hợp đồng đã COMPLETED → khách thuê xem được và báo đã thanh toán")
    void createBill_thenRenterViewsAndMarksAsPaid() {
        CompletedContract contract = ContractFixtures.createCompletedContract();

        Response createBillResponse = BillApi.createBill(
                contract.listing().host().accessToken(), contract.contractId(), 2_500_000d);
        assertThat(createBillResponse.statusCode())
                .as("Tạo hoá đơn thất bại: %s", createBillResponse.asString())
                .isEqualTo(200);
        long billId = createBillResponse.jsonPath().getLong("data.id");
        assertThat(billId).as("Hoá đơn vừa tạo phải có id").isPositive();

        Response viewResponse = BillApi.getAsRenter(contract.renter().accessToken(), billId);
        assertThat(viewResponse.statusCode())
                .as("Khách thuê không xem được hoá đơn của chính mình: %s", viewResponse.asString())
                .isEqualTo(200);

        Response markPaidResponse = BillApi.markPaidByRenter(contract.renter().accessToken(), billId);
        assertThat(markPaidResponse.statusCode())
                .as("Khách thuê báo đã thanh toán thất bại: %s", markPaidResponse.asString())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Người lạ (không phải khách thuê của hợp đồng) cố xem hoá đơn → phải bị từ chối")
    void viewBill_byUnrelatedUser_isRejected() {
        CompletedContract contract = ContractFixtures.createCompletedContract();
        Response createBillResponse = BillApi.createBill(
                contract.listing().host().accessToken(), contract.contractId(), 2_500_000d);
        assertThat(createBillResponse.statusCode()).isEqualTo(200);
        long billId = createBillResponse.jsonPath().getLong("data.id");

        var stranger = vn.rencity.qa.fixtures.AuthFixture.registerRenterAndLogin();
        Response viewResponse = BillApi.getAsRenter(stranger.accessToken(), billId);

        assertThat(viewResponse.statusCode())
                .as("Người không liên quan xem được hoá đơn của người khác là lỗ hổng lộ dữ liệu")
                .isNotEqualTo(200);
    }
}
