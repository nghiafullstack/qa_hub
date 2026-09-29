package vn.rencity.qa.fixtures;

import io.restassured.response.Response;
import vn.rencity.qa.client.ContractApi;

/** Dựng sẵn 1 hợp đồng đã COMPLETED từ đầu — dùng làm tiền đề cho test hoá đơn/chỉ số điện nước. */
public final class ContractFixtures {

    private ContractFixtures() {
    }

    public static CompletedContract createCompletedContract() {
        HostWithListing listing = MoPostFixtures.createHostWithListing();
        TestUser renter = AuthFixture.registerRenterAndLogin();

        Response createResponse = ContractApi.createContract(
                listing.host().accessToken(), listing.motelId(), listing.towerId(),
                renter.name(), renter.phoneNumber(), 2_500_000d);
        if (createResponse.statusCode() != 200) {
            throw new IllegalStateException(
                    "Tạo hợp đồng thất bại (status=" + createResponse.statusCode() + "): " + createResponse.asString());
        }
        long contractId = createResponse.jsonPath().getLong("data.id");

        Response paymentResponse = ContractApi.confirmPaymentAsRenter(renter.accessToken(), contractId);
        if (paymentResponse.statusCode() != 200) {
            throw new IllegalStateException(
                    "Xác nhận thanh toán thất bại (status=" + paymentResponse.statusCode() + "): "
                            + paymentResponse.asString());
        }

        return new CompletedContract(listing, renter, contractId);
    }
}
