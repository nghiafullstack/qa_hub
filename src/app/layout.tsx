import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "QA Hub",
  description: "Theo dõi kết quả test + phát hiện lệch nghiệp vụ đa dự án",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
