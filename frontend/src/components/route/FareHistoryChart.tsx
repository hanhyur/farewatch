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
import { formatKrw } from "@/lib/format";
import type { FareSnapshot } from "@/types/api";

interface FareHistoryChartProps {
  snapshots: FareSnapshot[];
}

interface ChartPoint {
  date: string;
  line1: string; // "오전" / "오후"
  line2: string; // "4/13"
  tooltipLabel: string;
  price: number;
}

const MAX_POINTS = 14; // 7일 x 하루 2회

function formatPoint(iso: string): Pick<ChartPoint, "line1" | "line2" | "tooltipLabel"> {
  const d = new Date(iso);
  const hour = d.getHours();
  const line1 = hour < 12 ? "오전" : "오후";
  const line2 = `${d.getMonth() + 1}/${d.getDate()}`;
  const tooltipLabel = new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "numeric",
    hour12: true,
  }).format(d);
  return { line1, line2, tooltipLabel };
}

function CustomXTick({
  x,
  y,
  payload,
}: {
  x: number;
  y: number;
  payload: { value: string; index: number };
}) {
  // value = "line1\nline2" 형태
  const [line1, line2] = (payload.value ?? "").split("\n");
  return (
    <g transform={`translate(${x},${y})`}>
      <text
        x={0}
        y={0}
        dy={12}
        textAnchor="middle"
        fill="var(--color-text-tertiary)"
        fontSize={11}
        fontWeight={500}
      >
        {line1}
      </text>
      <text
        x={0}
        y={0}
        dy={26}
        textAnchor="middle"
        fill="var(--color-text-tertiary)"
        fontSize={10}
      >
        {line2}
      </text>
    </g>
  );
}

export function FareHistoryChart({ snapshots }: FareHistoryChartProps) {
  const sorted = [...snapshots].sort(
    (a, b) =>
      new Date(a.collectedAt).getTime() - new Date(b.collectedAt).getTime(),
  );

  // 최근 14개만 (7일 x 2회/일)
  const recent = sorted.slice(-MAX_POINTS);

  const points: ChartPoint[] = recent.map((snapshot) => {
    const { line1, line2, tooltipLabel } = formatPoint(snapshot.collectedAt);
    return {
      date: `${line1}\n${line2}`,
      line1,
      line2,
      tooltipLabel,
      price: snapshot.price,
    };
  });

  if (points.length === 0) {
    return (
      <div className="flex h-80 items-center justify-center text-sm text-[var(--color-text-tertiary)]">
        표시할 가격 히스토리가 없어요
      </div>
    );
  }

  return (
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
            dataKey="date"
            tick={CustomXTick as never}
            tickLine={false}
            axisLine={false}
            height={40}
            interval={0}
          />
          <YAxis
            stroke="var(--color-text-tertiary)"
            fontSize={12}
            tickLine={false}
            axisLine={false}
            width={80}
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
            labelFormatter={(_label, payload) => {
              const point = payload?.[0]?.payload as ChartPoint | undefined;
              return point?.tooltipLabel ?? "";
            }}
          />
          <Area
            type="monotone"
            dataKey="price"
            stroke="var(--color-primary)"
            strokeWidth={2.5}
            fill="url(#fareGradient)"
            dot={{ r: 3, fill: "var(--color-primary)", strokeWidth: 0 }}
            activeDot={{ r: 5, fill: "var(--color-primary)", strokeWidth: 2, stroke: "white" }}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
