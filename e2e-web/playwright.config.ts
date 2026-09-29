import { defineConfig, devices } from '@playwright/test';

/**
 * URL trang web recity_web cần test — CHƯA CÓ giá trị mặc định thật, vì chưa xác nhận được URL
 * Dev/staging public của FE (file .env local của recity_web đang trỏ localhost:8180, không dùng
 * được cho CI). Set biến môi trường RENCITY_WEB_URL trước khi chạy, ví dụ:
 *   RENCITY_WEB_URL=https://dev.renapp.vn npx playwright test
 */
const baseURL = process.env.RENCITY_WEB_URL;

export default defineConfig({
  testDir: './tests',
  fullyParallel: false, // các luồng đăng nhập/đặt lịch có thể tạo dữ liệu chung — chạy tuần tự cho an toàn
  retries: process.env.CI ? 1 : 0,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL,
    trace: 'retain-on-failure', // xem lại được từng bước khi test fail, không cần đoán
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
