package vn.rencity.qa.fixtures;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sinh dữ liệu test ngẫu nhiên/duy nhất mỗi lần chạy — tránh đụng nhau khi chạy lặp lại nhiều
 * lần trên cùng 1 môi trường Dev (số điện thoại trùng → đăng ký lại tài khoản cũ; tên trùng →
 * lỗi "đã tồn tại" ở tower/motel/post — xem MoPostFixtures).
 */
public final class TestData {

    /** Mã hoá cả timestamp lẫn số thứ tự trong tiến trình — tránh trùng khi nhiều test chạy rất sát nhau. */
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private TestData() {
    }

    /** Số điện thoại VN giả hợp lệ theo regex {@code ^0[0-9]{9}$} — KHÔNG phải SĐT thật, chỉ dùng ở Dev. */
    public static String randomPhoneNumber() {
        // Đầu 09 + 8 số cuối lấy từ timestamp+sequence để gần như không bao giờ trùng trong 1 ngày
        // (giới hạn OTP: tối đa 5 lần gửi/SĐT/ngày — trùng SĐT giữa các lần chạy sẽ ăn hết quota).
        long unique = (System.currentTimeMillis() % 100_000_000L) + SEQUENCE.incrementAndGet();
        String digits = String.format("%08d", unique % 100_000_000L);
        return "09" + digits;
    }

    public static String randomSuffix() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString(ThreadLocalRandom.current().nextInt(1000, 9999), 36);
    }

    public static String uniqueName(String prefix) {
        return prefix + " " + randomSuffix();
    }

    /** Mật khẩu cố định đủ mạnh cho tài khoản test tự tạo — không cần bảo mật vì là tài khoản dùng 1 lần. */
    public static final String TEST_PASSWORD = "QaTest@12345";
}
