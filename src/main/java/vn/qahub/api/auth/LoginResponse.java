package vn.qahub.api.auth;

public record LoginResponse(String accessToken, String email, String role) {
}
