package vn.rencity.qa.fixtures;

/** 1 tài khoản test đã đăng ký + đăng nhập xong, sẵn sàng dùng {@code accessToken} làm header {@code token}. */
public record TestUser(String phoneNumber, String name, String password, boolean isHost, String accessToken) {
}
