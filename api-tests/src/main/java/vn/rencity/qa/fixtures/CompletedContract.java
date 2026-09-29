package vn.rencity.qa.fixtures;

/** 1 hợp đồng đã COMPLETED (khách thuê đã xác nhận thanh toán) — tiền đề cho test hoá đơn/chỉ số. */
public record CompletedContract(HostWithListing listing, TestUser renter, long contractId) {
}
