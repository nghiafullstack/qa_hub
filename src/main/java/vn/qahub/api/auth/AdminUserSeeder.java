package vn.qahub.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.qahub.api.user.User;
import vn.qahub.api.user.UserRepository;
import vn.qahub.api.user.UserRole;

/**
 * Tạo sẵn 1 tài khoản admin lúc khởi động lần đầu (bảng {@code users} rỗng) — hệ thống nội bộ,
 * không có luồng tự đăng ký, nên cần 1 cách để đăng nhập lần đầu. Set
 * {@code QAHUB_ADMIN_EMAIL}/{@code QAHUB_ADMIN_PASSWORD} trước khi chạy lần đầu; sau đó có thể
 * đổi mật khẩu qua DB trực tiếp hoặc (giai đoạn sau) 1 API đổi mật khẩu riêng.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${QAHUB_ADMIN_EMAIL:admin@qahub.local}")
    private String adminEmail;

    @Value("${QAHUB_ADMIN_PASSWORD:change-me}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
        log.warn("[QA_HUB] Đã tạo tài khoản admin đầu tiên: {} — ĐỔI MẬT KHẨU NGAY nếu đây là môi trường thật.",
                adminEmail);
    }
}
