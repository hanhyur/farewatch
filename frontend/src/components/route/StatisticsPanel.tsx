import { Card } from "@/components/ui/Card";
import { formatKrw } from "@/lib/format";
import type { FareStatistics } from "@/types/api";

interface StatisticsPanelProps {
  statistics: FareStatistics;
}

export function StatisticsPanel({ statistics }: StatisticsPanelProps) {
  const items = [
    { label: "평균", value: formatKrw(statistics.avgPrice) },
    { label: "최저", value: formatKrw(statistics.minPrice) },
    { label: "최고", value: formatKrw(statistics.maxPrice) },
    { label: "표본 수", value: `${statistics.sampleCount}건` },
    {
      label: "표준편차",
      value: statistics.stdDeviation ? formatKrw(statistics.stdDeviation) : "—",
    },
  ];

  return (
    <Card className="flex flex-wrap items-center gap-x-8 gap-y-3 p-5">
      {items.map((item) => (
        <div key={item.label}>
          <dt className="text-[11px] font-medium text-[var(--color-text-tertiary)]">
            {item.label}
          </dt>
          <dd className="text-base font-semibold tabular-nums">{item.value}</dd>
        </div>
      ))}
    </Card>
  );
}
