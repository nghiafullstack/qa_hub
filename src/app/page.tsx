"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import AuthGuard from "@/components/AuthGuard";
import TopNav from "@/components/TopNav";
import { api, Project } from "@/lib/api";

export default function HomePage() {
  return (
    <AuthGuard>
      <TopNav />
      <ProjectList />
    </AuthGuard>
  );
}

function ProjectList() {
  const [projects, setProjects] = useState<Project[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .listProjects()
      .then(setProjects)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-900">Dự án</h1>
        <Link
          href="/projects/new"
          className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          + Thêm dự án
        </Link>
      </div>

      {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      {projects === null && !error && <p className="text-slate-500">Đang tải...</p>}

      {projects?.length === 0 && (
        <p className="text-slate-500">Chưa có dự án nào — bấm &ldquo;+ Thêm dự án&rdquo; để bắt đầu.</p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {projects?.map((project) => (
          <Link
            key={project.id}
            href={`/projects/${project.slug}`}
            className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm hover:border-slate-400"
          >
            <h2 className="font-semibold text-slate-900">{project.name}</h2>
            <p className="mt-1 text-sm text-slate-500">{project.slug}</p>
            {project.description && <p className="mt-2 text-sm text-slate-600">{project.description}</p>}
          </Link>
        ))}
      </div>
    </main>
  );
}
