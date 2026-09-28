package vn.qahub.api.token;

/** {@code rawToken} CHỈ trả về đúng 1 lần ở response này — không lưu lại được sau đó, xem {@link ApiTokenService}. */
public record IssueTokenResponse(Long id, String name, String rawToken) {
}
