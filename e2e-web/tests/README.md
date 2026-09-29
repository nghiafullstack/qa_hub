# Thư mục test UI (Playwright)

Chưa có test thật ở đây. Cần trước khi viết:

1. **URL Dev/staging public của `recity_web`** — `.env` local của FE đang trỏ `localhost:8180`,
   không dùng được cho test tự động chạy trên CI. Cho biết URL thật (ví dụ Vercel preview URL,
   hay 1 domain riêng như `dev.renapp.vn`) thì mới viết/verify được test.
2. **1 tài khoản test cố định** (SĐT + mật khẩu) đã có sẵn trên môi trường đó để test đăng nhập —
   hoặc dùng lại chuỗi "gửi OTP → đăng ký" giống bên `api-tests` nếu form đăng ký trên web hỗ trợ
   tự động hoá được (một số form có captcha/chặn bot, cần kiểm tra riêng).

Khi có 2 thứ trên, việc tiếp theo là viết `login.spec.ts` làm test đầu tiên, sau đó mở rộng dần:
tìm phòng → xem chi tiết bài đăng → đặt lịch xem phòng — theo đúng thứ tự luồng người dùng thật
đi qua trên web.
