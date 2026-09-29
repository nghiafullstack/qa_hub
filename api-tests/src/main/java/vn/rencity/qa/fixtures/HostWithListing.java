package vn.rencity.qa.fixtures;

/**
 * 1 chủ nhà test đã có sẵn: 1 toà ({@code towerId}), 1 phòng thuộc toà đó ({@code motelId}), và
 * 1 bài đăng cho thuê phòng đó ({@code postId}) — đủ tiền đề cho cả test hợp đồng (cần
 * {@code tower_id}) lẫn CRM đặt lịch xem phòng.
 */
public record HostWithListing(TestUser host, Long towerId, Long motelId, Long postId) {
}
