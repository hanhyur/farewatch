"use client";

import {
  Area,
  AreaChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatKrw } from "@/lib/format";
import type { FareSnapshot, FareStatistics } from "@/types/api";

export type ChartPeriod = "1W" | "1M" | "6M" | "1Y";

interface FareHistoryChartProps {
  snapshots: FareSnapshot[];
  statistics?: FareStatistics | null;
  period: ChartPeriod;
  onPeriodChange: (period: ChartPeriod) => void;
  /** 전체 수집 데이터가 커버하는 일수 (탭 활성 판단용) */
  totalDataSpanDays?: number;
}

interface ChartPoint {
  ts: number; // epoch ms — X축 고유값
  tooltipLabel: string;
  price: number;
}

const PERIODS: { key: ChartPeriod; label: string; days: number }[] = [
  { key: "1W", label: "1주", days: 7 },
  { key: "1M", label: "1개월", days: 30 },
  { key: "6M", label: "6개월", days: 180 },
  { key: "1Y", label: "1년", days: 365 },
];

function formatTick(ts: number, period: ChartPeriod): string {
  const d = new Date(ts);
  const m = d.getMonth() + 1;
  const day = d.getDate();
  switch (period) {
    case "1W": {
      const hour = d.getHours();
      return hour < 12 ? `${m}/${day} AM` : `${m}/${day} PM`;
    }
    case "1M":
      return `${m}/${day}`;
    case "6M":
    case "1Y": {
      const y = d.getFullYear().toString().slice(2);
      return `${y}.${m}`;
    }
  }
}

function formatTooltipLabel(ts: number): string {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "numeric",
    hour12: true,
  }).format(new Date(ts));
}

/** 통계가 충분한 데이터를 갖고 있는지 (기준선 표시 여부) */
function hasEnoughData(stats: FareStatistics | null | undefined): boolean {
  return !!stats && stats.sampleCount >= 5;
}

export function FareHistoryChart({
  snapshots,
  statistics,
  period,
  onPeriodChange,
  totalDataSpanDays = 0,
}: FareHistoryChartProps) {
  const sorted = [...snapshots].sort(
    (a, b) =>
      new Date(a.collectedAt).getTime() - new Date(b.collectedAt).getTime(),
  );

  const points: ChartPoint[] = sorted.map((s) => ({
    ts: new Date(s.collectedAt).getTime(),
    tooltipLabel: formatTooltipLabel(new Date(s.collectedAt).getTime()),
    price: s.price,
  }));

  const showRefLines = hasEnoughData(statistics);
  const allTimeLow = showRefLines ? (statistics?.allTimeLowPrice ?? null) : null;
  const allTimeLowDate =
    allTimeLow !== null && statistics?.allTimeLowDate
      ? formatTooltipLabel(new Date(statistics.allTimeLowDate).getTime())
      : null;
  const avgPrice = showRefLines ? (statistics?.avgPrice ?? null) : null;

  // X축 틱 개수 제한
  const maxTicks = period === "1W" ? 14 : 10;
  const tickInterval =
    points.length <= maxTicks
      ? 0
      : Math.floor(points.length / maxTicks);

  return (
    <div className="flex flex-col gap-4">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-1">
          {PERIODS.map((p) => {
            // 1W는 항상 활성, 나머지는 데이터가 해당 기간 이상 쌓여야 활성
            const enabled = p.key === "1W" || totalDataSpanDays >= p.days * 0.5;
            return (
              <button
                key={p.key}
                type="button"
                onClick={() => enabled && onPeriodChange(p.key)}
                disabled={!enabled}
                className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
                  period === p.key
                    ? "bg-[var(--color-primary)] text-white"
                    : enabled
                      ? "bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] hover:bg-[var(--color-border)]"
                      : "bg-[var(--color-surface-muted)] text-[var(--color-text-tertiary)] opacity-40 cursor-not-allowed"
                }`}
              >
                {p.label}
              </button>
            );
          })}
        </div>

        {allTimeLow !== null && (
          <div className="flex items-center gap-2 text-xs">
            <span className="rounded-full bg-[var(--color-success-soft)] px-2.5 py-1 font-semibold text-[var(--color-success)]">
              역대 최저가
            </span>
            <span className="font-bold tabular-nums">
              {formatKrw(allTimeLow)}
            </span>
            {allTimeLowDate && (
              <span className="text-[var(--color-text-tertiary)]">
                ({allTimeLowDate})
              </span>
            )}
          </div>
        )}
      </div>

      {/* Chart */}
      {points.length === 0 ? (
        <div className="flex h-80 items-center justify-center text-sm text-[var(--color-text-tertiary)]">
          이 기간에 수집된 데이터가 없어요
        </div>
      ) : (
        <div className="h-80 w-full">
          <ResponsiveContainer width="100%" height={320} minWidth={0}>
            <AreaChart
              data={points}
              margin={{ top: 10, right: 12, bottom: 0, left: 0 }}
            >
              <defs>
                <linearGradient id="fareGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop
                    offset="0%"
                    stopColor="var(--color-primary)"
                    stopOpacity={0.25}
                  />
                  <stop
                    offset="100%"
                    stopColor="var(--color-primary)"
                    stopOpacity={0}
                  />
                </linearGradient>
              </defs>
              <CartesianGrid
                stroke="var(--color-border)"
                strokeDasharray="3 3"
                vertical={false}
              />
              <XAxis
                dataKey="ts"
                type="number"
                domain={["dataMin", "dataMax"]}
                scale="time"
                tickFormatter={(ts: number) => formatTick(ts, period)}
                stroke="var(--color-text-tertiary)"
                fontSize={11}
                tickLine={false}
                axisLine={false}
                height={30}
                interval={tickInterval}
              />
              <YAxis
                stroke="var(--color-text-tertiary)"
                fontSize={12}
                tickLine={false}
                axisLine={false}
                width={80}
                tickFormatter={(value: number) => formatKrw(value)}
                domain={["auto", "auto"]}
              />
              <Tooltip
                cursor={{ stroke: "var(--color-border-strong)" }}
                contentStyle={{
                  background: "var(--color-surface)",
                  border: "1px solid var(--color-border)",
                  borderRadius: 12,
                  fontSize: 13,
                }}
                formatter={(value) => [formatKrw(Number(value)), "가격"]}
                labelFormatter={(ts) => formatTooltipLabel(Number(ts))}
              />
              {allTimeLow !== null && (
                <ReferenceLine
                  y={allTimeLow}
                  stroke="var(--color-success)"
                  strokeDasharray="4 4"
                  strokeWidth={1.5}
                />
              )}
              {avgPrice !== null && (
                <ReferenceLine
                  y={avgPrice}
                  stroke="var(--color-warning)"
                  strokeDasharray="4 4"
                  strokeWidth={1}
                />
              )}
              <Area
                type="monotone"
                dataKey="price"
                stroke="var(--color-primary)"
                strokeWidth={2.5}
                fill="url(#fareGradient)"
                dot={
                  points.length <= 30
                    ? {
                        r: 3,
                        fill: "var(--color-primary)",
                        strokeWidth: 0,
                      }
                    : false
                }
                activeDot={{
                  r: 5,
                  fill: "var(--color-primary)",
                  strokeWidth: 2,
                  stroke: "white",
                }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Legend — 기준선이 있을 때만 */}
      {(allTimeLow !== null || avgPrice !== null) && (
        <div className="flex flex-wrap gap-4 text-[11px] text-[var(--color-text-tertiary)]">
          <span className="flex items-center gap-1.5">
            <span className="inline-block h-0.5 w-4 bg-[var(--color-primary)]" />
            가격
          </span>
          {avgPrice !== null && (
            <span className="flex items-center gap-1.5">
              <span className="inline-block h-0.5 w-4 border-t-2 border-dashed border-[var(--color-warning)]" />
              평균가 {formatKrw(avgPrice)}
            </span>
          )}
          {allTimeLow !== null && (
            <span className="flex items-center gap-1.5">
              <span className="inline-block h-0.5 w-4 border-t-2 border-dashed border-[var(--color-success)]" />
              역대 최저가
            </span>
          )}
        </div>
      )}
    </div>
  );
}
