"use client";

import type { Route } from "@/types/api";

interface RouteFilterProps {
  routes: Route[];
  originFilter: string;
  destinationFilter: string;
  verdictFilter: string;
  onOriginChange: (value: string) => void;
  onDestinationChange: (value: string) => void;
  onVerdictChange: (value: string) => void;
}

const SMALL_SELECT =
  "h-8 rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-xs text-[var(--color-text)] transition hover:border-[var(--color-border-strong)] focus-visible:border-[var(--color-primary)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-soft)]";

export function RouteFilter({
  routes,
  originFilter,
  destinationFilter,
  verdictFilter,
  onOriginChange,
  onDestinationChange,
  onVerdictChange,
}: RouteFilterProps) {
  const origins = [...new Set(routes.map((r) => r.origin))].sort();
  const destinations = [...new Set(routes.map((r) => r.destination))].sort();

  const verdictOptions: { value: string; label: string }[] = [
    { value: "", label: "전체 판단" },
    { value: "CHEAP", label: "지금 사세요" },
    { value: "FAIR", label: "적정가" },
    { value: "EXPENSIVE", label: "더 기다리세요" },
    { value: "INSUFFICIENT", label: "데이터 부족" },
  ];

  return (
    <div className="flex flex-wrap items-center gap-2">
      <select
        value={originFilter}
        onChange={(e) => onOriginChange(e.target.value)}
        className={SMALL_SELECT}
        aria-label="출발지 필터"
      >
        <option value="">전체 출발지</option>
        {origins.map((o) => (
          <option key={o} value={o}>
            {o}
          </option>
        ))}
      </select>

      <select
        value={destinationFilter}
        onChange={(e) => onDestinationChange(e.target.value)}
        className={SMALL_SELECT}
        aria-label="도착지 필터"
      >
        <option value="">전체 도착지</option>
        {destinations.map((d) => (
          <option key={d} value={d}>
            {d}
          </option>
        ))}
      </select>

      <select
        value={verdictFilter}
        onChange={(e) => onVerdictChange(e.target.value)}
        className={SMALL_SELECT}
        aria-label="판단 필터"
      >
        {verdictOptions.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  );
}
