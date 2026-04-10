"use client";

import { useQuery } from "@tanstack/react-query";
import { Card } from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { VerdictBadge } from "@/components/verdict/VerdictBadge";
import { fetchNotifications } from "@/lib/api/notifications";
import { formatDateTime, formatKrw } from "@/lib/format";

export default function NotificationsPage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["notifications"],
    queryFn: () => fetchNotifications(),
  });

  return (
    <div className="flex flex-col gap-8">
      <section className="flex flex-col gap-2">
        <p className="text-sm font-medium text-[var(--color-primary)]">
          Notifications
        </p>
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
          알림 이력
        </h1>
        <p className="max-w-xl text-[var(--color-text-secondary)]">
          등록한 알림 룰에 따라 발송된 이력을 확인할 수 있어요.
        </p>
      </section>

      {isLoading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 4 }).map((_, idx) => (
            <Card key={idx} className="p-5">
              <Skeleton className="h-5 w-48" />
              <Skeleton className="mt-3 h-4 w-full" />
            </Card>
          ))}
        </div>
      ) : isError ? (
        <Card className="p-8 text-center">
          <p className="font-semibold text-[var(--color-danger)]">
            알림 이력을 불러올 수 없어요
          </p>
          <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
            {error instanceof Error ? error.message : "알 수 없는 오류"}
          </p>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card className="p-8 text-center">
          <p className="font-semibold">발송된 알림이 없어요</p>
          <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
            대시보드에서 노선을 고르고 알림 룰을 등록해보세요.
          </p>
        </Card>
      ) : (
        <ul className="flex flex-col gap-3">
          {data.map((notification) => (
            <li key={notification.id}>
              <Card className="p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex flex-col gap-1.5">
                    <div className="flex items-center gap-2">
                      <VerdictBadge verdict={notification.verdict} size="sm" />
                      <span className="text-xs font-medium text-[var(--color-text-tertiary)]">
                        {notification.channel}
                      </span>
                    </div>
                    <p className="text-base font-semibold">
                      {notification.message}
                    </p>
                    <p className="text-xs text-[var(--color-text-tertiary)]">
                      노선 #{notification.routeId} · 출발{" "}
                      {notification.departureDate}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-bold tabular-nums">
                      {formatKrw(notification.priceAtSend)}
                    </p>
                    <p className="text-xs text-[var(--color-text-tertiary)]">
                      {formatDateTime(notification.sentAt)}
                    </p>
                  </div>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
