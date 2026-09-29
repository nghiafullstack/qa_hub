# QA Hub — Backend

Nền tảng nội bộ theo dõi lịch sử chạy test **cho nhiều dự án công ty** (không riêng Rencity), có
dashboard web, và engine phát hiện khi hành vi thực tế (qua kết quả test) khác tài liệu đặc tả
hoặc khác nhau giữa BE/FE.

**Đây là nhánh `backend`** của repo [qa_hub](https://github.com/nghiafullstack/qa_hub) — chỉ
chứa API (Spring Boot). Dashboard (Next.js) nằm ở **nhánh `frontend`** của cùng repo này, checkout
riêng để chạy song song. Hai nhánh không chia sẻ Git history con-trỏ-tới-nhau — mỗi nhánh tự vận
hành độc lập, chỉ liên hệ với nhau qua HTTP (`NEXT_PUBLIC_API_BASE_URL` bên frontend trỏ vào API
của nhánh này).

Repo này **hoàn toàn tách biệt** khỏi `rencity-platform-spring` — không dùng chung port, DB, hay
file docker-compose. Rencity chỉ là **dự án đầu tiên** được đăng ký vào hệ thống.

## API cung cấp

- `POST /api/v1/auth/login` — đăng nhập dashboard, trả JWT.
- `POST /api/v1/projects`, `GET /api/v1/projects`, `GET /api/v1/projects/{slug}` — quản lý dự án.
- `POST /api/v1/projects/{slug}/tokens` — cấp token riêng cho 1 dự án gửi kết quả test (hiện raw
  token đúng 1 lần).
- `POST /api/v1/ingest/{slug}/runs` — **Bearer = token dự án** (khác JWT đăng nhập), nhận multipart
  field `report` (1 hoặc nhiều file JUnit Surefire XML), tự parse ra test case, lưu `TestRun`.
  Đây là endpoint mà CI của dự án khác gọi sau khi chạy test xong — xem ví dụ tích hợp thật tại
  `rencity-qa-automation/.github/workflows/api-tests.yml`.
- `GET /api/v1/projects/{slug}/runs`, `GET /api/v1/projects/{slug}/runs/{runId}` — lịch sử/chi tiết run.
- `POST/GET /api/v1/projects/{slug}/documents` — upload tay, đồng bộ Git (`sync-git`), đăng ký
  OpenAPI URL.
- `POST /api/v1/projects/{slug}/runs/{runId}/analyze`, `GET .../analysis`, `GET .../findings`,
  `PUT /api/v1/findings/{id}/status` — engine phân tích (Gemini) + quản lý finding.

## Database

MySQL chạy trên **server riêng bên ngoài** — repo này không bundle container MySQL (Docker Compose
chỉ có service `api`). Trước khi chạy, tự tạo:
- 1 database/schema rỗng (tên khớp `DB_DATABASE`, mặc định `qa_hub`).
- 1 user có đủ quyền `CREATE`/`ALTER`/`INSERT`/`SELECT`/`UPDATE`/`DELETE` trên schema đó (Flyway
  cần quyền tạo bảng).

Flyway (`src/main/resources/db/migration/V1__init_schema.sql`) tự tạo 7 bảng khi API khởi động
lần đầu — không cần chạy SQL tay, chỉ cần schema rỗng + user đã tồn tại sẵn.

## Chạy local (không cần Docker)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home   # đổi theo máy bạn
DB_HOST=<host mysql của bạn> DB_PORT=3306 DB_DATABASE=qa_hub DB_USERNAME=qa_hub DB_PASSWORD=<mật khẩu> \
QAHUB_ADMIN_EMAIL=admin@qahub.local QAHUB_ADMIN_PASSWORD=Admin@12345 \
mvn spring-boot:run
# → chạy ở :9090, tự seed tài khoản admin đầu tiên từ QAHUB_ADMIN_EMAIL/PASSWORD
```

Sau đó checkout nhánh `frontend` (thư mục làm việc khác) để chạy dashboard trỏ vào `:9090`.

## Chạy bằng Docker Compose (chỉ backend: api)

```bash
cp .env.example .env
# điền DB_HOST/DB_PORT/DB_DATABASE/DB_USERNAME/DB_PASSWORD trỏ vào MySQL server ngoài của bạn,
# QAHUB_JWT_SECRET, QAHUB_ADMIN_PASSWORD, GEMINI_API_KEY (xem mục bên dưới),
# QAHUB_GITHUB_TOKEN (nếu cần), QAHUB_WEB_ORIGIN (origin thật của dashboard bên nhánh frontend)
docker compose up -d --build
```

api chạy ở `http://localhost:9090`. Cổng 9090 được chọn vì dải 8180–8186 đã bị
`rencity-platform-spring` chiếm (host network mode) trên cùng máy/VPS.

**Đã xác nhận** (2026-09-28): `docker build .` (Dockerfile ở root nhánh này) build image thành
công độc lập (`maven:3.9-eclipse-temurin-17` build stage → `eclipse-temurin:17-jre` runtime).
**Chưa xác nhận**: chạy image này trỏ vào MySQL server ngoài thật (mới verify với MySQL local qua
`mvn spring-boot:run`, xem mục "Trạng thái" bên dưới).

## Dùng lại API key Gemini của Rencity

Theo quyết định của PM: thay vì tạo key Claude/Gemini riêng cho qa-hub, dùng lại đúng key đã duyệt
cho module AI Chatbot bên `rencity-platform-spring` (xem `services/{mo-post,aff}-service/.env`,
biến `GEMINI_API_KEY`). Các biến `GEMINI_*` trong `.env.example` **cố ý đặt cùng tên** với bên
Rencity để copy thẳng giá trị, không cần đổi tên.

`GeminiClient`/`GeminiProperties` (`src/main/java/vn/qahub/api/analysis/`) được port trực tiếp từ
`vn.rencity.mopost.ai.client.GeminiClient` thật của Rencity (cùng cơ chế header `x-goog-api-key`,
cùng retry-on-timeout).

**Chưa xác nhận**: chưa có key thật trong session tạo dự án (đang chờ PM), nên đường gọi Gemini
thật mới chỉ verify được nhánh lỗi "chưa cấu hình GEMINI_API_KEY" — chưa verify nhánh sinh finding
thật.

## Bảo mật tài liệu (denylist)

`SecretPathDenylist` (`src/main/java/vn/qahub/api/document/`) chặn cứng các path khớp `.env*`,
`*.pem`, `*.key`, `*secret*`, `*credential*`, `id_rsa` — không cho đọc/lưu/gửi AI, kể cả khi đồng
bộ từ Git repo. Lý do: đã phát hiện thật `.env` của `rencity-platform-spring` (chứa connection
string MySQL production) nằm trong working tree lúc khảo sát repo để thiết kế tính năng này.

## Trạng thái đã verify vs chưa verify (tính đến 2026-09-28)

**Đã verify thật (chạy sống)**: ingest JUnit XML thật từ `rencity-qa-automation` (cả chạy tay lẫn
qua GitHub Actions), login JWT, CORS với dashboard, upload tài liệu tay + đăng ký OpenAPI URL,
`docker build` độc lập cho image này.

**Chưa verify**: gọi Gemini thật (chờ API key), đồng bộ tài liệu từ Git repo private của Rencity
(chờ `QAHUB_GITHUB_TOKEN`), chạy API trỏ vào MySQL server ngoài thật (chờ schema được tạo trên
server đó), job `deploy` trong CI (mặc định tắt, chờ VPS/secrets thật).

**Chưa làm** (đúng phạm vi MVP): SDK tích hợp sâu (capture request/response tự động — engine rule
so schema OpenAPI vì vậy còn hạn chế), multi-tenant/SaaS, tự động chạy AI sau mỗi run (vẫn theo
nút bấm), tích hợp Notion/Google Docs API trực tiếp, vector search tài liệu, biểu đồ xu hướng dài
hạn.
