"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import AuthGuard from "@/components/AuthGuard";
import TopNav from "@/components/TopNav";
import { api, ApiError, IssuedToken } from "@/lib/api";

export default function NewProjectPage() {
  return (
    <AuthGuard>
      <TopNav />
      <Form />
    </AuthGuard>
  );
}

function Form() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [gitRepoUrl, setGitRepoUrl] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [issuedToken, setIssuedToken] = useState<IssuedToken | null>(null);
  const [createdSlug, setCreatedSlug] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      const project = await api.createProject({ name, slug, gitRepoUrl: gitRepoUrl || undefined, description });
      const token = await api.issueToken(project.slug, "CI token đầu tiên");
      setIssuedToken(token);
      setCreatedSlug(project.slug);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Tạo dự án thất bại");
    }
  }

  if (issuedToken && createdSlug) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="mb-4 text-xl font-semibold text-slate-900">Đã tạo dự án — lưu token này lại!</h1>
        <p className="mb-2 text-sm text-slate-600">
          Token ingestion CHỈ hiện ra ĐÚNG 1 LẦN — copy ngay để cấu hình vào CI (biến môi trường bí mật,
          KHÔNG commit vào code):
        </p>
        <pre className="mb-4 overflow-x-auto rounded bg-slate-900 p-4 text-sm text-green-300">{issuedToken.rawToken}</pre>
        <button
          onClick={() => router.push(`/projects/${createdSlug}`)}
          className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          Đã lưu xong — vào dự án
        </button>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-8">
      <h1 className="mb-6 text-xl font-semibold text-slate-900">Thêm dự án mới</h1>
      <form onSubmit={handleSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <Field label="Tên dự án" value={name} onChange={setName} required />
        <Field
          label="Slug (dùng trong URL, chỉ chữ thường/số/dấu -)"
          value={slug}
          onChange={setSlug}
          required
          pattern="^[a-z0-9]+(-[a-z0-9]+)*$"
        />
        <Field
          label="Git repo URL (tuỳ chọn — dùng để đồng bộ tài liệu từ Git)"
          value={gitRepoUrl}
          onChange={setGitRepoUrl}
        />
        <div>
          <label className="block text-sm font-medium text-slate-700">Mô tả (tuỳ chọn)</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
          />
        </div>
        <button type="submit" className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800">
          Tạo dự án
        </button>
      </form>
    </main>
  );
}

function Field(props: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  required?: boolean;
  pattern?: string;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700">{props.label}</label>
      <input
        type="text"
        required={props.required}
        pattern={props.pattern}
        value={props.value}
        onChange={(e) => props.onChange(e.target.value)}
        className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
      />
    </div>
  );
}
