"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import AuthGuard from "@/components/AuthGuard";
import TopNav from "@/components/TopNav";
import StatusBadge from "@/components/StatusBadge";
import { api, Project, TestRun } from "@/lib/api";

export default function ProjectPage() {
  return (
    <AuthGuard>
      <TopNav />
      <ProjectRuns />
    </AuthGuard>
  );
}

function ProjectRuns() {
  const params = useParams<{ slug: string }>();
  const slug = params.slug;
  const [project, setProject] = useState<Project | null>(null);
  const [runs, setRuns] = useState<TestRun[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.getProject(slug).then(setProject).catch((e) => setError(e.message));
    api.listRuns(slug).then(setRuns).catch((e) => setError(e.message));
  }, [slug]);

  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-2 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">{project?.name ?? slug}</h1>
          <p className="text-sm text-slate-500">{project?.description}</p>
        </div>
        <Link
          href={`/projects/${slug}/documents`}
          className="rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
        >
          Tài liệu
        </Link>
      </div>

      {error && <p className="mt-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      <h2 className="mb-3 mt-8 text-lg font-medium text-slate-900">Lịch sử chạy test</h2>
      {runs === null && !error && <p className="text-slate-500">Đang tải...</p>}
      {runs?.length === 0 && (
        <p className="text-slate-500">
          Chưa có lần chạy nào — xem README của <code>rencity-qa-automation</code> để nối CI gửi kết quả vào đây.
        </p>
      )}

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2">#</th>
              <th className="px-4 py-2">Trạng thái</th>
              <th className="px-4 py-2">Môi trường</th>
              <th className="px-4 py-2">Tổng / Fail / Error</th>
              <th className="px-4 py-2">Nguồn</th>
              <th className="px-4 py-2">Bắt đầu</th>
            </tr>
          </thead>
          <tbody>
            {runs?.map((run) => (
              <tr key={run.id} className="border-t border-slate-100 hover:bg-slate-50">
                <td className="px-4 py-2">
                  <Link href={`/projects/${slug}/runs/${run.id}`} className="font-medium text-slate-900 hover:underline">
                    #{run.id}
                  </Link>
                </td>
                <td className="px-4 py-2">
                  <StatusBadge value={run.status} />
                </td>
                <td className="px-4 py-2 text-slate-600">{run.baseUrlTested ?? "-"}</td>
                <td className="px-4 py-2 text-slate-600">
                  {run.totalTests} / {run.totalFailures} / {run.totalErrors}
                </td>
                <td className="px-4 py-2 text-slate-600">{run.source}</td>
                <td className="px-4 py-2 text-slate-600">
                  {run.startedAt ? new Date(run.startedAt).toLocaleString("vi-VN") : "-"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  );
}
