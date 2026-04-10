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
  });

  return (
    <Link href={`/routes/${route.id}`} className="block">
      <Card interactive className="h-full p-6 flex flex-col gap-4">
        <header className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-medium text-[var(--color-text-tertiary)]">
              {route.airlineCode ?? "전체 항공사"}
            </p>
            <h3 className="text-xl font-semibold tracking-tight">
              {formatRouteName(route.origin, route.destination)}
            </h3>
          </div>
          {isLoading ? (
            <Skeleton className="h-7 w-24" />
          ) : judgment ? (
            <VerdictBadge verdict={judgment.verdict} size="sm" />
          ) : null}
        </header>

        {isLoading ? (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-8 w-32" />
            <Skeleton className="h-4 w-48" />
          </div>
        ) : isError || !judgment ? (
          <p className="text-sm text-[var(--color-text-tertiary)]">
            판단 데이터를 불러올 수 없어요
          </p>
        ) : (
          <>
            <div>
              <p className="text-3xl font-bold tracking-tight tabular-nums">
                {formatKrw(judgment.currentPrice)}
              </p>
              {judgment.avgPrice !== null && judgment.currentPrice !== null ? (
                <p className="text-sm text-[var(--color-text-secondary)] tabular-nums">
                  평균 대비{" "}
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
                  </span>{" "}
                  · 평균 {formatKrw(judgment.avgPrice)}
                </p>
              ) : (
                <p className="text-sm text-[var(--color-text-tertiary)]">
                  표본 {judgment.sampleCount}건
                </p>
              )}
            </div>
            <p className="text-sm text-[var(--color-text-secondary)] line-clamp-2">
              {judgment.suggestion}
            </p>
          </>
        )}
      </Card>
    </Link>
  );
}
