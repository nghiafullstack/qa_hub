# Rencity QA Automation

Bộ test tự động kiểm tra các luồng nghiệp vụ chính của Rencity — gọi thẳng API thật (không mock)
nhắm vào 1 môi trường thật (mặc định: **Dev**, `https://api-dev.renapp.vn`). Viết cho người
**không rành lĩnh vực test** cũng đọc/chạy/sửa được — mỗi test có tên + mô tả bằng tiếng Việt nói
rõ nó kiểm tra cái gì và tại sao.

Nằm chung nhánh `backend` với chính API của QA Hub (xem `README.md` ở cùng thư mục) — quản lý tập
trung 1 chỗ, nhưng vẫn là 2 project Maven độc lập (root là API QA Hub, `api-tests/` là project
Java riêng của bộ test này) và chỉ *nói chuyện* với QA Hub qua HTTP (gửi kết quả test vào endpoint
ingest, xem mục "CI/CD" bên dưới) — giống như bất kỳ dự án nào khác tích hợp với QA Hub, không có
gì đặc quyền chỉ vì cùng nhánh.

## Bộ test này KHÔNG phải là gì

- **Không phải unit test.** Không test 1 hàm/1 class riêng lẻ trong code — test bằng cách gọi API
  y như một người dùng/app thật gọi, kiểm tra kết quả cuối cùng đúng hay sai.
- **Không mock bất cứ thứ gì.** Mỗi lần chạy sẽ **tạo dữ liệu thật** trên môi trường đang test
  (tài khoản mới, phòng mới, hợp đồng mới...) — xem mục "Dữ liệu test để lại" bên dưới.
- **Chưa phải test UI (giao diện web).** Phần này (`e2e-web/`, dùng Playwright) mới scaffold sườn,
  chưa có test thật — cần bạn cho biết URL môi trường Dev/staging thật của `recity_web` (file
  `.env` local đang trỏ `localhost:8180`, không phải URL public) thì mới viết/verify được.

## Cấu trúc

```
api-tests/              # Java 17 + JUnit 5 + REST-assured — ĐÃ CÓ, đã chạy thật, đã pass
  └── src/
      ├── main/java/vn/rencity/qa/
      │   ├── config/     # Base URL (mặc định Dev, đổi được)
      │   ├── client/     # 1 class/domain: AuthApi, MoPostApi, ContractApi, CrmApi, WalletApi, BillApi
      │   └── fixtures/   # Dựng sẵn dữ liệu test (đăng ký tài khoản, tạo phòng+bài đăng, tạo hợp đồng...)
      └── test/java/vn/rencity/qa/flows/
            AuthFlowTest.java              # Đăng ký / đăng nhập / OTP
            ContractFlowTest.java          # Hợp đồng thuê phòng — ĐANG BỊ CHẶN, xem bên dưới
            BillingFlowTest.java           # Chỉ số điện nước / hoá đơn — ĐANG BỊ CHẶN, xem bên dưới
            CrmReservationFlowTest.java    # CRM đặt lịch xem phòng
            WalletWithdrawalFlowTest.java  # Ví tiền / rút tiền (1 phần)
e2e-web/                # Playwright + TypeScript — CHỈ MỚI SƯỜN, chưa có test thật
.github/workflows/
  api-tests.yml         # Chạy api-tests theo lịch (2h sáng VN mỗi ngày) + chạy tay
  ci-deploy.yml         # Của riêng API QA Hub — không liên quan tới bộ test này
```

## Chạy thử ở máy bạn

Cần Java 17 + Maven. Mặc định chạy nhắm Dev, không cần cấu hình gì thêm:

```bash
cd api-tests
mvn test
```

Muốn đổi môi trường (vd test local hoặc 1 domain khác):

```bash
mvn test -Drencity.base-url=http://localhost:8180
# hoặc set biến môi trường (ưu tiên hơn), tiện cho CI:
RENCITY_BASE_URL=http://localhost:8180 mvn test
```

Chạy 1 file/1 test riêng lẻ (nhanh hơn khi đang sửa):

```bash
mvn test -Dtest=AuthFlowTest
mvn test -Dtest=AuthFlowTest#registerThenLogin_succeeds
```

## Kết quả đã chạy thật (2026-09-28, nhắm Dev)

| Luồng | File | Kết quả |
|---|---|---|
| Đăng ký / đăng nhập / OTP | `AuthFlowTest` | ✅ 4/4 pass |
| CRM đặt lịch xem phòng | `CrmReservationFlowTest` | ✅ 2/2 pass |
| Ví tiền — gửi OTP + các trường hợp phải bị chặn | `WalletWithdrawalFlowTest` | ✅ 3/3 pass |
| Hợp đồng thuê phòng | `ContractFlowTest` | 🔴 1/2 fail — **lỗi thật trên Dev**, không phải lỗi test |
| Hoá đơn / chỉ số điện nước | `BillingFlowTest` | 🔴 Bị chặn — phụ thuộc hợp đồng COMPLETED ở trên |

### 🔴 Lỗi thật phát hiện được trên Dev (chưa fix)

Bước **khách thuê xác nhận thanh toán hợp đồng** (`PUT /api/user/community/contracts/{id}`) trả
về lỗi 500:

```json
{"code":500,"msg":"Table 'rencity_prod.config_gift_coins' doesn't exist","msg_code":"INTERNAL_ERROR","success":false}
```

Nghĩa là **mọi lượt khách thuê xác nhận thanh toán trên Dev hiện đang lỗi 500** — code
(`GiftCoinService`, thưởng xu sau khi thanh toán hợp đồng) cần bảng `config_gift_coins`, nhưng
bảng này không tồn tại trong DB mà Dev đang trỏ vào (Spring dùng `ddl-auto: none`, không tự tạo
bảng — bảng phải có sẵn từ trước hoặc tạo tay). Việc cần làm:

1. Tạo bảng `config_gift_coins` trên DB Dev (và ít nhất 1 dòng cấu hình `is_active=1` để tính
   năng thưởng xu hoạt động đúng, không chỉ là hết lỗi 500).
2. Đáng chú ý: tên DB trong lỗi là **`rencity_prod`** — nên xác nhận lại Dev có đang trỏ đúng DB
   dự kiến hay không (có thể chỉ là quy ước đặt tên cũ, nhưng nên chắc chắn).

`ContractFlowTest`/`BillingFlowTest` sẽ tự pass lại khi lỗi này được fix — không cần sửa gì
trong bộ test.

## Dữ liệu test để lại trên môi trường

Mỗi lần chạy tạo mới: 1-2 tài khoản (SĐT giả dạng `09xxxxxxxx`, không phải SĐT thật), có thể kèm
1 toà + 1 phòng + 1 bài đăng (`QA Tower ...`/`QA Room ...`/`QA Post ...`) + 1 hợp đồng. Dữ liệu
này **không tự xoá** sau khi test chạy xong — nếu cần dọn định kỳ, có thể lọc theo tiền tố tên
(`QA Tower`, `QA Room`, `QA Post`, `QA Host`, `QA Renter`) hoặc theo đầu số điện thoại `09` sinh
từ timestamp. Chưa viết script dọn tự động — nêu ra đây để bạn quyết có cần không.

## Những gì CHƯA làm được (cần dữ liệu/quyết định thêm)

- **Rút tiền thành công (happy path).** Chỉ test được "gửi OTP thành công" và "rút tiền khi ví
  0 đồng / chưa liên kết ngân hàng phải bị từ chối". Test rút tiền **thành công thật** cần 1 tài
  khoản có sẵn số dư (`golden_coin`/`silver_coin` > 0) và đã liên kết ngân hàng (`bank_id` thật)
  — không tự dựng được từ 1 tài khoản đăng ký mới hoàn toàn (không có cách tự nạp tiền qua API
  công khai). Nếu muốn có test này, cần bạn cung cấp 1 tài khoản test cố định đã seed sẵn số dư +
  bank_id trên Dev, hoặc tôi tìm 1 con đường nạp tiền qua API nội bộ khác.
- **UI test (Playwright, thư mục `e2e-web/`).** Chỉ mới tạo sườn thư mục, chưa có test thật —
  cần bạn cho URL Dev/staging thật của `recity_web` (không phải `localhost` trong `.env` local).

## CI/CD

`.github/workflows/api-tests.yml`: chạy `api-tests` (chưa đụng tới `e2e-web`) theo lịch mỗi đêm
lúc 2h sáng giờ VN + chạy tay qua tab **Actions → API Tests (BE flows) → Run workflow** (có thể
nhập `base_url` khác Dev nếu cần). Kết quả hiện ở Summary của mỗi lần chạy; log chi tiết khi fail
tải về ở mục Artifacts (`surefire-reports`).

Lịch chạy mỗi đêm (`schedule`) giờ hoạt động bình thường vì file này nằm trên nhánh `backend` —
**đúng là default branch** của repo `qa_hub` (khác với lúc bộ test này còn ở nhánh `rencity-tests`
riêng, khi đó cron không tự chạy được).

Sau khi test xong, kết quả (Surefire XML) được gửi qua HTTP vào chính API QA Hub cùng nhánh này
(`POST /api/v1/ingest/rencity/runs`, xem `README.md`) để lưu lịch sử + có thể chạy phân tích lệch
nghiệp vụ bằng AI. Cần secrets `QAHUB_API_URL` + `QAHUB_PROJECT_TOKEN` trong Settings > Secrets
của repo — thiếu thì bước này tự bỏ qua, không làm fail job test.

**Nâng cấp sau (chưa làm, theo đúng quyết định ban đầu — ưu tiên đơn giản trước):** tự động kích
hoạt ngay sau khi `rencity-platform-spring` deploy Dev xong, thay vì chỉ chạy theo lịch. Cần thêm
bước gọi `repository_dispatch` từ `deploy-dev.yml` (BE) sang repo này, kèm Personal Access Token
chia sẻ giữa 2 repo.
