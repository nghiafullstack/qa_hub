import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Không cần rewrites/proxy — client gọi thẳng NEXT_PUBLIC_API_BASE_URL của qa-hub/api.
  output: "standalone", // image Docker gọn hơn nhiều — chỉ copy đúng phần cần chạy, không cả node_modules dev.
};

export default nextConfig;
