"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { clearSession, getEmail } from "@/lib/auth";

export default function TopNav() {
  const router = useRouter();

  function logout() {
    clearSession();
    router.replace("/login");
  }

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3">
        <Link href="/" className="text-lg font-semibold text-slate-900">
          QA Hub
        </Link>
        <div className="flex items-center gap-4 text-sm text-slate-600">
          <span>{typeof window !== "undefined" ? getEmail() : ""}</span>
          <button onClick={logout} className="rounded border border-slate-300 px-3 py-1 hover:bg-slate-100">
            Đăng xuất
          </button>
        </div>
      </div>
    </header>
  );
}
