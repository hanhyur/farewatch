"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Card } from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { VerdictBadge } from "@/components/verdict/VerdictBadge";
import { fetchJudgment } from "@/lib/api/routes";
import { formatKrw, formatRouteName, percentDiff, formatPercent } from "@/lib/format";
import type { Route } from "@/types/api";

interface RouteCardProps {
  route: Route;
}

export function RouteCard({ route }: RouteCardProps) {
  const { data: judgment, isLoading, isError } = useQuery({
    queryKey: ["judgment", route.id],
    queryFn: () => fetchJudgment(route.id),
    refetchInterval: 5 * 60 * 1000,
  });

  return (
    <Link href={`/routes/${route.id}`} className="block">
      <Card interactive className="p-5">
        {/* Top: Route name + badge */}
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--color-surface-muted)] text-[10px] font-bold text-[var(--color-text-tertiary)]">
              {route.airlineCode ?? "ALL"}
            </span>
            <h3 className="text-lg font-semibold tracking-tight">
              {formatRouteName(route.origin, route.destination)}
            </h3>
          </div>
          {isLoading ? (
            <Skeleton className="h-6 w-20" />
          ) : judgment ? (
            <VerdictBadge verdict={judgment.verdict} size="sm" />
          ) : null}
        </div>

        {/* Bottom: Price + detail */}
        <div className="mt-3">
          {isLoading ? (
            <Skeleton className="h-7 w-32" />
          ) : isError || !judgment ? (
            <p className="text-sm text-[var(--color-text-tertiary)]">
              데이터를 불러올 수 없어요
            </p>
          ) : (
            <div className="flex items-baseline justify-between gap-4">
              <p className="text-2xl font-bold tracking-tight tabular-nums">
                {formatKrw(judgment.currentPrice)}
              </p>
              {judgment.avgPrice !== null && judgment.currentPrice !== null ? (
                <p className="text-xs text-[var(--color-text-secondary)] tabular-nums">
                  평균{" "}
                  <span
                    className={
                      judgment.currentPrice < judgment.avgPrice
                        ? "text-[var(--color-success)] font-semibold"
                        : "text-[var(--color-danger)] font-semibold"
                    }
                  >
                    {formatPercent(
                      percentDiff(judgment.currentPrice, judgment.avgPrice),
                      1,
                    )}
                  </span>
                  {" · "}
                  {formatKrw(judgment.avgPrice)}
                </p>
              ) : (
                <p className="text-xs text-[var(--color-text-tertiary)]">
                  표본 {judgment.sampleCount}건
                </p>
              )}
            </div>
          )}
        </div>

        {/* Suggestion — single line */}
        {judgment?.suggestion && (
          <p className="mt-2 truncate text-xs text-[var(--color-text-secondary)]">
            {judgment.suggestion}
          </p>
        )}
      </Card>
    </Link>
  );
}
