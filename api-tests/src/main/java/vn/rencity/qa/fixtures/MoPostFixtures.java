package vn.rencity.qa.fixtures;

import io.restassured.response.Response;
import vn.rencity.qa.client.MoPostApi;

/**
 * Dựng sẵn "1 chủ nhà có 1 toà, 1 phòng trong toà đó, 1 bài đăng cho phòng đó" từ đầu — dùng làm
 * tiền đề cho test hợp đồng và CRM đặt lịch xem phòng (2 flow đó đều validate
 * {@code tower_id}/{@code motel_id}/{@code mo_post_id} phải tồn tại thật qua mo-post-service,
 * không nhận id tuỳ ý).
 */
public final class MoPostFixtures {

    private MoPostFixtures() {
    }

    public static HostWithListing createHostWithListing() {
        TestUser host = AuthFixture.registerHostAndLogin();

        Response towerResponse = MoPostApi.createTower(host.accessToken(), TestData.uniqueName("QA Tower"));
        if (towerResponse.statusCode() != 200) {
            throw new IllegalStateException(
                    "Tạo toà thất bại (status=" + towerResponse.statusCode() + "): " + towerResponse.asString());
        }
        Long towerId = towerResponse.jsonPath().getLong("data.id");

        Response motelResponse = MoPostApi.createMotel(
                host.accessToken(), TestData.uniqueName("QA Room"), 2_500_000d, towerId);
        if (motelResponse.statusCode() != 200) {
            throw new IllegalStateException(
                    "Tạo phòng thất bại (status=" + motelResponse.statusCode() + "): " + motelResponse.asString());
        }
        Long motelId = motelResponse.jsonPath().getLong("data.id");

        Response postResponse = MoPostApi.createPost(host.accessToken(), TestData.uniqueName("QA Post"), motelId);
        if (postResponse.statusCode() != 200) {
            throw new IllegalStateException(
                    "Tạo bài đăng thất bại (status=" + postResponse.statusCode() + "): " + postResponse.asString());
        }
        Long postId = postResponse.jsonPath().getLong("data.id");

        return new HostWithListing(host, towerId, motelId, postId);
    }
}
