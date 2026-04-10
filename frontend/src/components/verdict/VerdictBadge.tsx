import type { Verdict } from "@/types/api";

interface VerdictBadgeProps {
  verdict: Verdict;
  size?: "sm" | "md" | "lg";
}

interface VerdictStyle {
  label: string;
  bg: string;
  fg: string;
  dot: string;
}

const STYLES: Record<Verdict, VerdictStyle> = {
  CHEAP: {
    label: "지금 사세요",
    bg: "bg-[var(--color-success-soft)]",
    fg: "text-[var(--color-success)]",
    dot: "bg-[var(--color-success)]",
  },
  FAIR: {
    label: "적정가",
    bg: "bg-[var(--color-warning-soft)]",
    fg: "text-[var(--color-warning)]",
    dot: "bg-[var(--color-warning)]",
  },
  EXPENSIVE: {
    label: "더 기다리세요",
    bg: "bg-[var(--color-danger-soft)]",
    fg: "text-[var(--color-danger)]",
    dot: "bg-[var(--color-danger)]",
  },
  INSUFFICIENT: {
    label: "데이터 부족",
    bg: "bg-[var(--color-neutral-soft)]",
    fg: "text-[var(--color-neutral)]",
    dot: "bg-[var(--color-neutral)]",
  },
};

const SIZES = {
  sm: "text-xs px-2.5 py-1 gap-1.5",
  md: "text-sm px-3 py-1.5 gap-2",
  lg: "text-base px-4 py-2 gap-2",
};

export function VerdictBadge({ verdict, size = "md" }: VerdictBadgeProps) {
  const style = STYLES[verdict];
  return (
    <span
      className={`inline-flex items-center rounded-full font-semibold ${style.bg} ${style.fg} ${SIZES[size]}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${style.dot}`} />
      {style.label}
    </span>
  );
}
