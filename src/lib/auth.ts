"use client";

/**
 * Lưu JWT ở localStorage — đủ dùng cho công cụ nội bộ, không cần httpOnly cookie/SSR session
 * phức tạp cho bản MVP. Mất khi xoá dữ liệu trình duyệt là chấp nhận được (đăng nhập lại).
 */
const TOKEN_KEY = "qahub_token";
const EMAIL_KEY = "qahub_email";
const ROLE_KEY = "qahub_role";

export function saveSession(token: string, email: string, role: string) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(EMAIL_KEY, email);
  localStorage.setItem(ROLE_KEY, role);
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function getEmail(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(EMAIL_KEY);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EMAIL_KEY);
  localStorage.removeItem(ROLE_KEY);
}
