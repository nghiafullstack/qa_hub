package vn.rencity.qa.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.restassured.response.Response;

/**
 * Chỉ số điện nước + hoá đơn — bc-service. Hợp đồng phải đang ở trạng thái 2 (COMPLETED), 4,
 * hoặc 6 mới ghi được chỉ số ({@code MotelMonthlyServiceManageService}), nên luôn cần 1 hợp
 * đồng đã COMPLETED trước (xem {@code ContractApi}).
 */
public final class BillApi {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private BillApi() {
    }

    public static Response recordUtilityIndex(
            String hostToken, long contractId, long motelId,
            int electricPrevious, int electricCurrent, int waterPrevious, int waterCurrent) {
        LocalDate today = LocalDate.now();
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("contract_id", contractId);
        body.put("motel_id", motelId);
        body.put("payment_period", today.format(DATE));
        body.put("record_date", today.format(DATE));
        body.put("electric_previous_index", electricPrevious);
        body.put("electric_current_index", electricCurrent);
        body.put("water_previous_index", waterPrevious);
        body.put("water_current_index", waterCurrent);
        return ApiRequests.post("/api/user/manage/motel-monthly-services", hostToken, body);
    }

    public static Response createBill(String hostToken, long contractId, double totalMoneyMotel) {
        LocalDate today = LocalDate.now();
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("contract_id", contractId);
        body.put("payment_period", today.format(DATE));
        body.put("total_money_motel", totalMoneyMotel);
        body.put("discount", 0);
        body.put("total_final", totalMoneyMotel);
        return ApiRequests.post("/api/user/manage/bills", hostToken, body);
    }

    public static Response getAsRenter(String renterToken, long billId) {
        return ApiRequests.get("/api/user/community/bills/" + billId, renterToken);
    }

    public static Response markPaidByRenter(String renterToken, long billId) {
        // BillStatus.WAIT_FOR_CONFIRM = 1 — khách thuê báo "tôi đã trả tiền mặt", chờ chủ nhà xác nhận.
        return ApiRequests.put("/api/user/community/bills/" + billId, renterToken, java.util.Map.of("status", 1));
    }
}
