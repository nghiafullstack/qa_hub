"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import AuthGuard from "@/components/AuthGuard";
import TopNav from "@/components/TopNav";
import StatusBadge from "@/components/StatusBadge";
import { api, AnalysisRun, ApiError, Finding, TestCase, TestRunDetail } from "@/lib/api";

export default function RunDetailPage() {
  return (
    <AuthGuard>
      <TopNav />
      <RunDetail />
    </AuthGuard>
  );
}

function RunDetail() {
  const params = useParams<{ slug: string; runId: string }>();
  const slug = params.slug;
  const runId = Number(params.runId);

  const [detail, setDetail] = useState<TestRunDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<number | null>(null);

  const [analysisRuns, setAnalysisRuns] = useState<AnalysisRun[]>([]);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [analyzing, setAnalyzing] = useState(false);
  const [analysisError, setAnalysisError] = useState<string | null>(null);

  const loadAnalysis = useCallback(async () => {
    const runs = await api.listAnalysisRuns(slug, runId);
    setAnalysisRuns(runs);
    const latest = runs[0];
    if (latest && latest.status === "DONE") {
      const f = await api.listFindings(slug, runId, latest.id);
      setFindings(f);
    } else {
      setFindings([]);
    }
  }, [slug, runId]);

  useEffect(() => {
    api.getRunDetail(slug, runId).then(setDetail).catch((e) => setError(e.message));
    loadAnalysis().catch(() => {});
  }, [slug, runId, loadAnalysis]);

  async function handleAnalyze() {
    setAnalyzing(true);
    setAnalysisError(null);
    try {
      await api.analyze(slug, runId);
      await loadAnalysis();
    } catch (e) {
      setAnalysisError(e instanceof ApiError ? e.message : "Phân tích thất bại");
    } finally {
      setAnalyzing(false);
    }
  }

  const latestAnalysis = analysisRuns[0];
  const findingsByTestCase = new Map<number, Finding[]>();
  for (const f of findings) {
    if (f.evidenceTestCaseId == null) continue;
    const list = findingsByTestCase.get(f.evidenceTestCaseId) ?? [];
    list.push(f);
    findingsByTestCase.set(f.evidenceTestCaseId, list);
  }
  const generalFindings = findings.filter((f) => f.evidenceTestCaseId == null);

  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <Link href={`/projects/${slug}`} className="text-sm text-slate-500 hover:underline">
        ← Quay lại danh sách run
      </Link>

      {error && <p className="mt-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      {detail && (
        <>
          <div className="mt-2 flex items-center justify-between">
            <h1 className="text-2xl font-semibold text-slate-900">
              Run #{detail.run.id} <StatusBadge value={detail.run.status} />
            </h1>
            <button
              onClick={handleAnalyze}
              disabled={analyzing}
              className="rounded bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
            >
              {analyzing ? "Đang phân tích..." : "Phân tích"}
            </button>
          </div>
          <p className="text-sm text-slate-500">
            {detail.run.baseUrlTested} · {detail.run.totalTests} test, {detail.run.totalFailures} fail,{" "}
            {detail.run.totalErrors} error
          </p>

          {analysisError && <p className="mt-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{analysisError}</p>}
          {latestAnalysis && latestAnalysis.status === "ERROR" && (
            <p className="mt-3 rounded bg-amber-50 px-3 py-2 text-sm text-amber-800">
              Lần phân tích gần nhất lỗi: {latestAnalysis.errorMessage}
            </p>
          )}
          {latestAnalysis && latestAnalysis.status === "DONE" && findings.length === 0 && (
            <p className="mt-3 rounded bg-green-50 px-3 py-2 text-sm text-green-800">
              Đã phân tích — không phát hiện lệch nghiệp vụ nào có căn cứ từ tài liệu.
            </p>
          )}

          {generalFindings.length > 0 && (
            <div className="mt-4 space-y-2">
              {generalFindings.map((f) => (
                <FindingCard key={f.id} finding={f} slug={slug} runId={runId} onChanged={loadAnalysis} />
              ))}
            </div>
          )}

          <h2 className="mb-3 mt-8 text-lg font-medium text-slate-900">Danh sách test</h2>
          <div className="space-y-2">
            {detail.testCases.map((tc) => (
              <TestCaseRow
                key={tc.id}
                testCase={tc}
                expanded={expanded === tc.id}
                onToggle={() => setExpanded(expanded === tc.id ? null : tc.id)}
                findings={findingsByTestCase.get(tc.id) ?? []}
                slug={slug}
                runId={runId}
                onFindingChanged={loadAnalysis}
              />
            ))}
          </div>
        </>
      )}
    </main>
  );
}

function TestCaseRow(props: {
  testCase: TestCase;
  expanded: boolean;
  onToggle: () => void;
  findings: Finding[];
  slug: string;
  runId: number;
  onFindingChanged: () => void;
}) {
  const { testCase: tc } = props;
  const isBad = tc.status === "FAILED" || tc.status === "ERROR";
  return (
    <div className={`rounded-lg border bg-white ${isBad ? "border-red-200" : "border-slate-200"}`}>
      <button onClick={props.onToggle} className="flex w-full items-center justify-between px-4 py-3 text-left">
        <div>
          <span className="font-mono text-sm text-slate-900">{tc.methodName}</span>
          <span className="ml-2 text-xs text-slate-400">{tc.className}</span>
        </div>
        <div className="flex items-center gap-2">
          {tc.businessFlowTag && (
            <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{tc.businessFlowTag}</span>
          )}
          {props.findings.length > 0 && (
            <span className="rounded bg-indigo-100 px-2 py-0.5 text-xs text-indigo-700">
              {props.findings.length} phát hiện
            </span>
          )}
          <StatusBadge value={tc.status} />
        </div>
      </button>
      {props.expanded && (
        <div className="border-t border-slate-100 px-4 py-3 text-sm">
          {tc.failureMessage && (
            <pre className="mb-2 overflow-x-auto whitespace-pre-wrap rounded bg-red-50 p-3 text-xs text-red-800">
              {tc.failureMessage}
            </pre>
          )}
          {props.findings.map((f) => (
            <FindingCard key={f.id} finding={f} slug={props.slug} runId={props.runId} onChanged={props.onFindingChanged} />
          ))}
        </div>
      )}
    </div>
  );
}

function FindingCard(props: { finding: Finding; slug: string; runId: number; onChanged: () => void }) {
  const { finding: f } = props;
  const [updating, setUpdating] = useState(false);

  async function setStatus(status: Finding["status"]) {
    setUpdating(true);
    try {
      await api.updateFindingStatus(props.slug, props.runId, f.id, status);
      props.onChanged();
    } finally {
      setUpdating(false);
    }
  }

  return (
    <div className="mb-2 rounded border border-indigo-200 bg-indigo-50 p-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <StatusBadge value={f.severity} />
          <span className="text-xs text-slate-500">{f.category}</span>
          {f.aiGenerated && <span className="text-xs text-slate-400">(AI)</span>}
        </div>
        <StatusBadge value={f.status} />
      </div>
      <p className="mt-1 font-medium text-slate-900">{f.title}</p>
      {f.description && <p className="mt-1 whitespace-pre-wrap text-sm text-slate-700">{f.description}</p>}
      {f.status === "OPEN" && (
        <div className="mt-2 flex gap-2">
          <button
            disabled={updating}
            onClick={() => setStatus("RESOLVED")}
            className="rounded border border-green-300 bg-white px-2 py-1 text-xs text-green-700 hover:bg-green-50"
          >
            Đánh dấu đã xử lý
          </button>
          <button
            disabled={updating}
            onClick={() => setStatus("WONTFIX")}
            className="rounded border border-slate-300 bg-white px-2 py-1 text-xs text-slate-600 hover:bg-slate-50"
          >
            Bỏ qua
          </button>
        </div>
      )}
    </div>
  );
}
