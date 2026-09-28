"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import AuthGuard from "@/components/AuthGuard";
import TopNav from "@/components/TopNav";
import { api, ApiError, DocumentItem } from "@/lib/api";

export default function DocumentsPage() {
  return (
    <AuthGuard>
      <TopNav />
      <Documents />
    </AuthGuard>
  );
}

function Documents() {
  const params = useParams<{ slug: string }>();
  const slug = params.slug;
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const [gitBranch, setGitBranch] = useState("developer");
  const [gitGlobs, setGitGlobs] = useState("docs/*.md");
  const [syncing, setSyncing] = useState(false);

  const [openApiUrl, setOpenApiUrl] = useState("https://api-dev.renapp.vn/v3/api-docs/bc");
  const [registering, setRegistering] = useState(false);

  function reload() {
    api.listDocuments(slug).then(setDocuments).catch((e) => setError(e.message));
  }

  useEffect(reload, [slug]);

  async function handleUpload() {
    const file = fileInputRef.current?.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      await api.uploadDocument(slug, file);
      if (fileInputRef.current) fileInputRef.current.value = "";
      reload();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Upload thất bại");
    } finally {
      setUploading(false);
    }
  }

  async function handleSyncGit() {
    setSyncing(true);
    setError(null);
    try {
      const globs = gitGlobs.split(",").map((s) => s.trim()).filter(Boolean);
      await api.syncGit(slug, gitBranch, globs);
      reload();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Đồng bộ Git thất bại");
    } finally {
      setSyncing(false);
    }
  }

  async function handleRegisterOpenApi() {
    setRegistering(true);
    setError(null);
    try {
      await api.registerOpenApi(slug, openApiUrl);
      reload();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Đăng ký OpenAPI thất bại");
    } finally {
      setRegistering(false);
    }
  }

  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <Link href={`/projects/${slug}`} className="text-sm text-slate-500 hover:underline">
        ← Quay lại dự án
      </Link>
      <h1 className="mb-6 mt-2 text-2xl font-semibold text-slate-900">Tài liệu — {slug}</h1>

      {error && <p className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      <div className="mb-8 grid gap-4 md:grid-cols-3">
        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-2 font-medium text-slate-900">Upload tay</h2>
          <p className="mb-2 text-xs text-slate-500">.md / .txt / .json / .pdf / .docx</p>
          <input ref={fileInputRef} type="file" className="mb-2 w-full text-sm" />
          <button
            onClick={handleUpload}
            disabled={uploading}
            className="w-full rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
          >
            {uploading ? "Đang tải lên..." : "Tải lên"}
          </button>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-2 font-medium text-slate-900">Đồng bộ từ Git</h2>
          <p className="mb-2 text-xs text-slate-500">Repo private cần cấu hình QAHUB_GITHUB_TOKEN ở server.</p>
          <input
            value={gitBranch}
            onChange={(e) => setGitBranch(e.target.value)}
            placeholder="Nhánh (vd developer)"
            className="mb-2 w-full rounded border border-slate-300 px-2 py-1 text-sm"
          />
          <input
            value={gitGlobs}
            onChange={(e) => setGitGlobs(e.target.value)}
            placeholder="Glob, cách nhau dấu phẩy (vd docs/*.md)"
            className="mb-2 w-full rounded border border-slate-300 px-2 py-1 text-sm"
          />
          <button
            onClick={handleSyncGit}
            disabled={syncing}
            className="w-full rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
          >
            {syncing ? "Đang đồng bộ..." : "Đồng bộ"}
          </button>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-2 font-medium text-slate-900">Đăng ký OpenAPI</h2>
          <p className="mb-2 text-xs text-slate-500">Chỉ đọc được khi SPRINGDOC_ENABLED=true (Dev/local).</p>
          <input
            value={openApiUrl}
            onChange={(e) => setOpenApiUrl(e.target.value)}
            className="mb-2 w-full rounded border border-slate-300 px-2 py-1 text-sm"
          />
          <button
            onClick={handleRegisterOpenApi}
            disabled={registering}
            className="w-full rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
          >
            {registering ? "Đang đăng ký..." : "Đăng ký"}
          </button>
        </div>
      </div>

      <h2 className="mb-3 text-lg font-medium text-slate-900">Đã nạp ({documents.length})</h2>
      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2">Tiêu đề</th>
              <th className="px-4 py-2">Nguồn</th>
              <th className="px-4 py-2">Định dạng</th>
              <th className="px-4 py-2">Độ dài</th>
              <th className="px-4 py-2">Đồng bộ lần cuối</th>
            </tr>
          </thead>
          <tbody>
            {documents.map((doc) => (
              <tr key={doc.id} className="border-t border-slate-100">
                <td className="px-4 py-2 text-slate-900">{doc.title}</td>
                <td className="px-4 py-2 text-slate-600">{doc.sourceType}</td>
                <td className="px-4 py-2 text-slate-600">{doc.docFormat}</td>
                <td className="px-4 py-2 text-slate-600">{doc.contentLength.toLocaleString("vi-VN")} ký tự</td>
                <td className="px-4 py-2 text-slate-600">
                  {doc.lastSyncedAt ? new Date(doc.lastSyncedAt).toLocaleString("vi-VN") : "-"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  );
}
