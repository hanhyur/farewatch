import { formatKrw } from "@/lib/format";
import type { FareStatistics } from "@/types/api";

interface StatisticsPanelProps {
  statistics: FareStatistics;
}

interface StatItem {
  label: string;
  value: string;
}

export function StatisticsPanel({ statistics }: StatisticsPanelProps) {
  const items: StatItem[] = [
    { label: "평균", value: formatKrw(statistics.avgPrice) },
    { label: "최저", value: formatKrw(statistics.minPrice) },
    { label: "최고", value: formatKrw(statistics.maxPrice) },
    {
      label: "표준편차",
      value: statistics.stdDeviation
        ? formatKrw(statistics.stdDeviation)
        : "—",
    },
    {
      label: "P25",
      value: statistics.p25Price !== null ? formatKrw(statistics.p25Price) : "—",
    },
    {
      label: "P75",
      value: statistics.p75Price !== null ? formatKrw(statistics.p75Price) : "—",
    },
  ];

  return (
    <dl className="grid grid-cols-2 gap-5 sm:grid-cols-3">
      {items.map((item) => (
        <div key={item.label} className="flex flex-col gap-1">
          <dt className="text-xs font-medium text-[var(--color-text-tertiary)]">
            {item.label}
          </dt>
          <dd className="text-lg font-semibold tabular-nums">{item.value}</dd>
        </div>
      ))}
      <div className="flex flex-col gap-1">
        <dt className="text-xs font-medium text-[var(--color-text-tertiary)]">
          표본 수
        </dt>
        <dd className="text-lg font-semibold tabular-nums">
          {statistics.sampleCount}건
        </dd>
      </div>
    </dl>
  );
}
