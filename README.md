# QA Hub — Frontend

Dashboard (Next.js) của QA Hub — nền tảng nội bộ theo dõi lịch sử chạy test cho nhiều dự án công
ty, có engine phát hiện khi hành vi thực tế (qua kết quả test) khác tài liệu đặc tả hoặc khác nhau
giữa BE/FE.

**Đây là nhánh `frontend`** của repo [qa_hub](https://github.com/nghiafullstack/qa_hub) — chỉ
chứa dashboard. API (Spring Boot) nằm ở **nhánh `backend`** của cùng repo này, checkout riêng để
chạy song song. Dashboard này chỉ nói chuyện với backend qua HTTP
(`NEXT_PUBLIC_API_BASE_URL`) — không chia sẻ code hay Git history với nhánh `backend`.

## Các trang

- `/login` — đăng nhập (JWT).
- `/` — danh sách dự án.
- `/projects/new` — tạo dự án mới, hiện token ingest 1 lần duy nhất.
- `/projects/[slug]` — lịch sử các lần chạy test của 1 dự án.
- `/projects/[slug]/runs/[runId]` — chi tiết 1 run: danh sách test, lỗi cụ thể, nút "Phân tích",
  danh sách Finding (có thể Resolve/Ignore).
- `/projects/[slug]/documents` — upload tài liệu tay, đồng bộ từ Git repo, đăng ký OpenAPI URL.

## Chạy local

Cần **backend đang chạy trước** (xem `README.md` ở nhánh `backend`, mặc định `:9090`).

```bash
cp .env.local.example .env.local   # NEXT_PUBLIC_API_BASE_URL=http://localhost:9090
npm install
npm run dev
# → :3000, đăng nhập bằng tài khoản admin đã seed bên backend
```

## Chạy bằng Docker

```bash
QAHUB_API_PUBLIC_URL=http://localhost:9090 docker compose up -d --build
```

→ `http://localhost:9091`. Lưu ý: `NEXT_PUBLIC_API_BASE_URL` của Next.js chỉ đọc được **lúc build
image**, không đọc lại lúc container chạy — đổi URL backend nghĩa là phải build lại image
(`docker compose up -d --build` lại), không chỉ restart container.

**Đã xác nhận** (2026-09-28): `docker build .` (Dockerfile ở root nhánh này, Node 22-alpine,
Next.js `output: "standalone"`) build image thành công độc lập. Toàn bộ luồng UI (login, tạo dự
án, xem lịch sử run thật từ Rencity, upload tài liệu, đăng ký OpenAPI URL) đã verify sống qua
browser khi chạy cùng backend local.

## Trạng thái chưa verify

- `docker compose up -d` chạy image này trỏ vào 1 backend đã deploy thật qua mạng (chỉ mới verify
  build image thành công + chạy `npm run dev` trỏ vào backend local).
- Job `deploy` trong CI (mặc định tắt, chờ VPS/secrets thật — xem `.github/workflows/ci-deploy.yml`).
- Luồng "Phân tích" hiện finding thật từ Gemini (backend chưa có API key thật — xem README nhánh
  `backend`).
