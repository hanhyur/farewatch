"use client";

import { useQuery } from "@tanstack/react-query";
import { RouteCard } from "@/components/route/RouteCard";
import { Card } from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { fetchRoutes } from "@/lib/api/routes";

export default function DashboardPage() {
  const { data: routes, isLoading, isError, error } = useQuery({
    queryKey: ["routes"],
    queryFn: fetchRoutes,
  });

  return (
    <div className="flex flex-col gap-8">
      <section className="flex flex-col gap-2">
        <p className="text-sm font-medium text-[var(--color-primary)]">
          FareWatch Dashboard
        </p>
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
          지금 사도 되는 항공권인가요?
        </h1>
        <p className="max-w-xl text-[var(--color-text-secondary)]">
          가격 히스토리를 분석해서 지금 사야 할지, 기다려야 할지 알려드려요.
          관심 있는 노선을 골라보세요.
        </p>
      </section>

      {isLoading ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, idx) => (
            <Card key={idx} className="h-[180px] p-6 flex flex-col gap-3">
              <Skeleton className="h-5 w-24" />
              <Skeleton className="h-7 w-40" />
              <Skeleton className="h-4 w-full mt-auto" />
            </Card>
          ))}
        </div>
      ) : isError ? (
        <Card className="p-8 text-center">
          <p className="text-[var(--color-danger)] font-semibold">
            노선을 불러올 수 없어요
          </p>
          <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
            {error instanceof Error ? error.message : "알 수 없는 오류"}
          </p>
        </Card>
      ) : !routes || routes.length === 0 ? (
        <Card className="p-8 text-center">
          <p className="font-semibold">등록된 노선이 없어요</p>
          <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
            백엔드가 실행 중인지 확인해주세요.
          </p>
        </Card>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {routes.map((route) => (
            <RouteCard key={route.id} route={route} />
          ))}
        </div>
      )}
    </div>
  );
}
