"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueries } from "@tanstack/react-query";
import { RouteCard } from "@/components/route/RouteCard";
import { RouteFilter } from "@/components/route/RouteFilter";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { fetchRoutes, fetchJudgment } from "@/lib/api/routes";
import type { Verdict } from "@/types/api";

export default function DashboardPage() {
  const [originFilter, setOriginFilter] = useState("");
  const [destinationFilter, setDestinationFilter] = useState("");
  const [verdictFilter, setVerdictFilter] = useState("");

  const {
    data: routes,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["routes"],
    queryFn: fetchRoutes,
  });

  const activeRoutes = routes?.filter((r) => r.active) ?? [];

  const judgmentQueries = useQueries({
    queries: activeRoutes.map((route) => ({
      queryKey: ["judgment", route.id],
      queryFn: () => fetchJudgment(route.id),
      refetchInterval: 5 * 60 * 1000,
    })),
  });

  const verdictMap = new Map<number, Verdict>();
  activeRoutes.forEach((route, idx) => {
    const data = judgmentQueries[idx]?.data;
    if (data) {
      verdictMap.set(route.id, data.verdict);
    }
  });

  const filteredRoutes = activeRoutes.filter((route) => {
    if (originFilter && route.origin !== originFilter) return false;
    if (destinationFilter && route.destination !== destinationFilter)
      return false;
    if (verdictFilter) {
      const verdict = verdictMap.get(route.id);
      if (!verdict || verdict !== verdictFilter) return false;
    }
    return true;
  });

  return (
    <div className="flex flex-col gap-5">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
            지금 사도 되는 항공권인가요?
          </h1>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            가격 히스토리를 분석해서 지금 사야 할지, 기다려야 할지 알려드려요.
          </p>
        </div>
        <Link href="/search" className="shrink-0">
          <Button variant="primary" size="md">
            항공편 검색
          </Button>
        </Link>
      </div>

      {/* Filters — inline */}
      {!isLoading && routes && routes.length > 0 && (
        <RouteFilter
          routes={routes}
          originFilter={originFilter}
          destinationFilter={destinationFilter}
          verdictFilter={verdictFilter}
          onOriginChange={setOriginFilter}
          onDestinationChange={setDestinationFilter}
          onVerdictChange={setVerdictFilter}
        />
      )}

      {/* Content */}
      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2">
          {Array.from({ length: 4 }).map((_, idx) => (
            <Card key={idx} className="p-5 flex flex-col gap-3">
              <Skeleton className="h-5 w-24" />
              <Skeleton className="h-7 w-40" />
              <Skeleton className="h-4 w-full" />
            </Card>
          ))}
        </div>
      ) : isError ? (
        <Card className="p-6 text-center">
          <p className="text-[var(--color-danger)] font-semibold">
            노선을 불러올 수 없어요
          </p>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            {error instanceof Error ? error.message : "알 수 없는 오류"}
          </p>
        </Card>
      ) : filteredRoutes.length === 0 ? (
        <Card className="p-6 text-center">
          <p className="font-semibold">
            {routes && routes.length > 0
              ? "필터 조건에 맞는 노선이 없어요"
              : "등록된 노선이 없어요"}
          </p>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            {routes && routes.length > 0
              ? "필터를 변경하거나 새 노선을 검색해보세요."
              : "검색 페이지에서 노선을 등록해보세요."}
          </p>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {filteredRoutes.map((route) => (
            <RouteCard key={route.id} route={route} />
          ))}
        </div>
      )}
    </div>
  );
}
