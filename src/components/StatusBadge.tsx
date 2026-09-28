const COLORS: Record<string, string> = {
  PASSED: "bg-green-100 text-green-800",
  DONE: "bg-green-100 text-green-800",
  RESOLVED: "bg-green-100 text-green-800",
  FAILED: "bg-red-100 text-red-800",
  ERROR: "bg-red-100 text-red-800",
  SKIPPED: "bg-slate-100 text-slate-600",
  PENDING: "bg-amber-100 text-amber-800",
  RUNNING: "bg-amber-100 text-amber-800",
  OPEN: "bg-amber-100 text-amber-800",
  WONTFIX: "bg-slate-100 text-slate-600",
  INFO: "bg-blue-100 text-blue-800",
  WARN: "bg-amber-100 text-amber-800",
  CRITICAL: "bg-red-100 text-red-800",
};

export default function StatusBadge({ value }: { value: string }) {
  const cls = COLORS[value] ?? "bg-slate-100 text-slate-600";
  return <span className={`rounded px-2 py-0.5 text-xs font-medium ${cls}`}>{value}</span>;
}
