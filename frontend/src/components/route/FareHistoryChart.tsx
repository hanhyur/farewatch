"use client";

import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatDate, formatKrw } from "@/lib/format";
import type { FareSnapshot } from "@/types/api";

interface FareHistoryChartProps {
  snapshots: FareSnapshot[];
}

interface ChartPoint {
  date: string;
  label: string;
  price: number;
}

export function FareHistoryChart({ snapshots }: FareHistoryChartProps) {
  const points: ChartPoint[] = [...snapshots]
    .sort(
      (a, b) =>
        new Date(a.collectedAt).getTime() - new Date(b.collectedAt).getTime(),
    )
    .map((snapshot) => ({
      date: snapshot.collectedAt,
      label: formatDate(snapshot.collectedAt),
      price: snapshot.price,
    }));

  if (points.length === 0) {
    return (
      <div className="flex h-64 items-center justify-center text-sm text-[var(--color-text-tertiary)]">
        표시할 가격 히스토리가 없어요
      </div>
    );
  }

  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height={256} minWidth={0}>
        <AreaChart data={points} margin={{ top: 10, right: 12, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id="fareGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--color-primary)" stopOpacity={0.25} />
              <stop offset="100%" stopColor="var(--color-primary)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid
            stroke="var(--color-border)"
            strokeDasharray="3 3"
            vertical={false}
          />
          <XAxis
            dataKey="label"
            stroke="var(--color-text-tertiary)"
            fontSize={12}
            tickLine={false}
            axisLine={false}
          />
          <YAxis
            stroke="var(--color-text-tertiary)"
            fontSize={12}
            tickLine={false}
            axisLine={false}
            width={72}
            tickFormatter={(value: number) => formatKrw(value)}
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
            labelFormatter={(label) => String(label ?? "")}
          />
          <Area
            type="monotone"
            dataKey="price"
            stroke="var(--color-primary)"
            strokeWidth={2.5}
            fill="url(#fareGradient)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
