"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getToken } from "@/lib/auth";

/** Chặn render trang cho tới khi xác nhận có JWT — chưa có thì đá về /login. */
export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }
    setReady(true);
  }, [router]);

  if (!ready) {
    return <div className="p-8 text-slate-500">Đang kiểm tra đăng nhập...</div>;
  }
  return <>{children}</>;
}
