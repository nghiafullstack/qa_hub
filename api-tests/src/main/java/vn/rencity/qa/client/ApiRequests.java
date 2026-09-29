package vn.rencity.qa.client;

import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import vn.rencity.qa.config.TestConfig;

/**
 * Lớp gọi HTTP dùng chung cho mọi domain client (Auth/MoPost/Contract/Crm/Wallet/Bill) —
 * KHÔNG gọi trực tiếp {@code RestAssured.given()} rải rác ở từng test, để dễ đổi 1 chỗ (vd thêm
 * header chung, đổi timeout) mà không phải sửa từng file.
 *
 * <p>Xác thực dùng header {@code token} (JWT thô, KHÔNG có tiền tố "Bearer ") — khớp
 * {@code AuthUserResolver}/{@code BcAuthUserResolver}/{@code CrmAuthUserResolver}/
 * {@code MoPostAuthUserResolver} bên BE, tất cả đều đọc {@code request.getHeader("token")}.
 */
public final class ApiRequests {

    private ApiRequests() {
    }

    private static RequestSpecification base() {
        return RestAssured.given()
                .baseUri(TestConfig.baseUrl())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    private static RequestSpecification withToken(String token) {
        RequestSpecification spec = base();
        if (token != null && !token.isBlank()) {
            spec = spec.header("token", token);
        }
        return spec;
    }

    public static Response get(String path, String token) {
        return withToken(token).get(path);
    }

    public static Response get(String path, String token, Map<String, ?> queryParams) {
        return withToken(token).queryParams(queryParams).get(path);
    }

    public static Response post(String path, String token, Object body) {
        return withToken(token).body(body).post(path);
    }

    public static Response put(String path, String token, Object body) {
        RequestSpecification spec = withToken(token);
        if (body != null) {
            spec = spec.body(body);
        }
        return spec.put(path);
    }
}
