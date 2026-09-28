"use client";

import { getToken } from "./auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:9090";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  requireAuth = true,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (requireAuth) {
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }
  if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  if (!response.ok) {
    let message = `Lỗi ${response.status}`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
    } catch {
      // body không phải JSON — giữ message mặc định
    }
    throw new ApiError(response.status, message);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

// ---- Types khớp response bên api (Jackson mặc định camelCase, KHÁC quy ước snake_case của Rencity) ----

export interface LoginResponse {
  accessToken: string;
  email: string;
  role: string;
}

export interface Project {
  id: number;
  name: string;
  slug: string;
  gitRepoUrl: string | null;
  description: string | null;
  createdAt: string;
}

export interface IssuedToken {
  id: number;
  name: string;
  rawToken: string;
}

export interface TestRun {
  id: number;
  projectId: number;
  baseUrlTested: string | null;
  gitCommitSha: string | null;
  source: string;
  status: "PENDING" | "PASSED" | "FAILED" | "ERROR";
  totalTests: number;
  totalFailures: number;
  totalErrors: number;
  totalSkipped: number;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface TestCase {
  id: number;
  className: string;
  methodName: string;
  displayName: string;
  status: "PASSED" | "FAILED" | "ERROR" | "SKIPPED";
  durationMs: number | null;
  failureMessage: string | null;
  stackTrace: string | null;
  businessFlowTag: string | null;
}

export interface TestRunDetail {
  run: TestRun;
  testCases: TestCase[];
}

export interface DocumentItem {
  id: number;
  sourceType: "GIT_PATH" | "UPLOAD" | "OPENAPI_URL";
  sourceRef: string | null;
  title: string | null;
  docFormat: string | null;
  contentLength: number;
  lastSyncedAt: string | null;
}

export interface AnalysisRun {
  id: number;
  testRunId: number;
  status: "PENDING" | "RUNNING" | "DONE" | "ERROR";
  modelUsed: string | null;
  errorMessage: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface Finding {
  id: number;
  category: "SCHEMA_MISMATCH" | "BUSINESS_LOGIC_DRIFT" | "BE_FE_MISMATCH";
  severity: "INFO" | "WARN" | "CRITICAL";
  title: string;
  description: string | null;
  evidenceTestCaseId: number | null;
  aiGenerated: boolean;
  status: "OPEN" | "RESOLVED" | "WONTFIX";
  createdAt: string;
}

export const api = {
  login: (email: string, password: string) =>
    request<LoginResponse>("/api/v1/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }, false),

  listProjects: () => request<Project[]>("/api/v1/projects"),
  getProject: (slug: string) => request<Project>(`/api/v1/projects/${slug}`),
  createProject: (input: { name: string; slug: string; gitRepoUrl?: string; description?: string }) =>
    request<Project>("/api/v1/projects", { method: "POST", body: JSON.stringify(input) }),

  issueToken: (slug: string, name: string) =>
    request<IssuedToken>(`/api/v1/projects/${slug}/tokens`, { method: "POST", body: JSON.stringify({ name }) }),

  listRuns: (slug: string) => request<TestRun[]>(`/api/v1/projects/${slug}/runs`),
  getRunDetail: (slug: string, runId: number) =>
    request<TestRunDetail>(`/api/v1/projects/${slug}/runs/${runId}`),

  listDocuments: (slug: string) => request<DocumentItem[]>(`/api/v1/projects/${slug}/documents`),
  getDocumentDetail: (slug: string, id: number) =>
    request<DocumentItem & { contentText: string | null }>(`/api/v1/projects/${slug}/documents/${id}`),
  uploadDocument: (slug: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return request<DocumentItem>(`/api/v1/projects/${slug}/documents/upload`, { method: "POST", body: form });
  },
  syncGit: (slug: string, branch: string, globs: string[]) =>
    request<DocumentItem[]>(`/api/v1/projects/${slug}/documents/sync-git`, {
      method: "POST",
      body: JSON.stringify({ branch, globs }),
    }),
  registerOpenApi: (slug: string, url: string) =>
    request<DocumentItem>(`/api/v1/projects/${slug}/documents/openapi`, {
      method: "POST",
      body: JSON.stringify({ url }),
    }),

  analyze: (slug: string, runId: number) =>
    request<AnalysisRun>(`/api/v1/projects/${slug}/runs/${runId}/analyze`, { method: "POST" }),
  listAnalysisRuns: (slug: string, runId: number) =>
    request<AnalysisRun[]>(`/api/v1/projects/${slug}/runs/${runId}/analysis`),
  listFindings: (slug: string, runId: number, analysisRunId: number) =>
    request<Finding[]>(`/api/v1/projects/${slug}/runs/${runId}/analysis/${analysisRunId}/findings`),
  updateFindingStatus: (slug: string, runId: number, findingId: number, status: Finding["status"]) =>
    request<Finding>(`/api/v1/projects/${slug}/runs/${runId}/findings/${findingId}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    }),
};
