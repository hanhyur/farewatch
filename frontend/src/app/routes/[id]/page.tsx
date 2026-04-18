"use client";

import { useState } from "react";
import Link from "next/link";
import { use } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card } from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { VerdictBadge } from "@/components/verdict/VerdictBadge";
import { FareHistoryChart, type ChartPeriod } from "@/components/route/FareHistoryChart";
import { StatisticsPanel } from "@/components/route/StatisticsPanel";
import { AlertRuleForm } from "@/components/alert/AlertRuleForm";
import {
  fetchFares,
  fetchJudgment,
  fetchRoute,
  fetchStatistics,
} from "@/lib/api/routes";
import { formatKrw, formatRouteName } from "@/lib/format";

function todayPlusDays(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return d.toISOString().split("T")[0];
}

const PERIOD_DAYS: Record<ChartPeriod, number> = {
  "1W": 7,
  "1M": 30,
  "6M": 180,
  "1Y": 365,
};

interface RouteDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function RouteDetailPage({ params }: RouteDetailPageProps) {
  const { id } = use(params);
  const routeId = Number(id);

  const [departureDate, setDepartureDate] = useState(todayPlusDays(7));
  const [chartPeriod, setChartPeriod] = useState<ChartPeriod>("1W");

  const route = useQuery({
    queryKey: ["route", routeId],
    queryFn: () => fetchRoute(routeId),
    enabled: Number.isFinite(routeId),
  });
  const judgment = useQuery({
    queryKey: ["judgment", routeId, departureDate],
    queryFn: () => fetchJudgment(routeId, departureDate),
    enabled: Number.isFinite(routeId),
    refetchInterval: 5 * 60 * 1000,
  });
  const fares = useQuery({
    queryKey: ["fares", routeId, departureDate, chartPeriod],
    queryFn: () => fetchFares(routeId, departureDate, { days: PERIOD_DAYS[chartPeriod] }),
    enabled: Number.isFinite(routeId),
  });
  // 전체 데이터 범위 파악용 (탭 활성/비활성 판단)
  const allFares = useQuery({
    queryKey: ["fares-all", routeId, departureDate],
    queryFn: () => fetchFares(routeId, departureDate, { days: 365 }),
    enabled: Number.isFinite(routeId),
    staleTime: 60_000,
  });
  const statistics = useQuery({
    queryKey: ["statistics", routeId, departureDate],
    queryFn: () => fetchStatistics(routeId, departureDate),
    enabled: Number.isFinite(routeId),
    retry: false,
  });

  return (
    <div className="flex flex-col gap-8">
      <Link
        href="/"
        className="text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text)]"
      >
        ← 대시보드로
      </Link>

      <Card className="p-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex flex-col gap-3">
            {route.data ? (
              <>
                <p className="text-xs font-medium text-[var(--color-text-tertiary)]">
                  {route.data.airlineCode ?? "전체 항공사"}
                </p>
                <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
                  {formatRouteName(
                    route.data.origin,
                    route.data.destination,
                  )}
                </h1>
              </>
            ) : (
              <>
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-10 w-72" />
              </>
            )}
          </div>
          <div className="flex flex-col items-start gap-3 sm:items-end">
            {judgment.data ? (
              <>
                <VerdictBadge verdict={judgment.data.verdict} size="lg" />
                <p className="text-3xl font-bold tabular-nums">
                  {formatKrw(judgment.data.currentPrice)}
                </p>
              </>
            ) : (
              <Skeleton className="h-12 w-40" />
            )}
          </div>
        </div>
        {judgment.data?.suggestion ? (
          <p className="mt-6 text-[var(--color-text-secondary)]">
            {judgment.data.suggestion}
          </p>
        ) : null}
      </Card>

      {/* 출발일 선택 */}
      <div className="flex items-center gap-3">
        <label
          htmlFor="departureDate"
          className="text-sm font-medium text-[var(--color-text-secondary)]"
        >
          출발일
        </label>
        <input
          id="departureDate"
          type="date"
          value={departureDate}
          onChange={(e) => setDepartureDate(e.target.value)}
          min={todayPlusDays(0)}
          className="form-input w-auto"
        />
      </div>

      {/* 통계 */}
      {statistics.isLoading ? (
        <Card className="p-5">
          <div className="flex gap-6">
            {Array.from({ length: 5 }).map((_, idx) => (
              <Skeleton key={idx} className="h-10 w-20" />
            ))}
          </div>
        </Card>
      ) : statistics.data ? (
        <StatisticsPanel statistics={statistics.data} />
      ) : null}

      {/* 차트 */}
      <Card className="p-6">
        <h2 className="mb-4 text-lg font-semibold tracking-tight">
          가격 히스토리
        </h2>
        {fares.isLoading ? (
          <Skeleton className="h-80 w-full" />
        ) : fares.isError || !fares.data ? (
          <p className="text-sm text-[var(--color-danger)]">
            가격 히스토리를 불러올 수 없어요
          </p>
        ) : (
          <FareHistoryChart
            snapshots={fares.data}
            statistics={statistics.data ?? null}
            period={chartPeriod}
            onPeriodChange={setChartPeriod}
            totalDataSpanDays={
              allFares.data && allFares.data.length >= 2
                ? (new Date(allFares.data[allFares.data.length - 1].collectedAt).getTime() -
                    new Date(allFares.data[0].collectedAt).getTime()) /
                  (1000 * 60 * 60 * 24)
                : 0
            }
          />
        )}
      </Card>

      <Card className="p-6">
        <h2 className="text-lg font-semibold tracking-tight">알림 설정</h2>
        <p className="mb-5 text-sm text-[var(--color-text-secondary)]">
          원하는 조건을 만족하면 알림을 보내드릴게요.
        </p>
        <AlertRuleForm routeId={routeId} />
      </Card>
    </div>
  );
}
