import type { HTMLAttributes } from "react";

type CardProps = HTMLAttributes<HTMLDivElement> & {
  interactive?: boolean;
};

export function Card({
  className = "",
  interactive = false,
  ...rest
}: CardProps) {
  const base =
    "rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-sm)]";
  const hover = interactive
    ? "transition hover:-translate-y-0.5 hover:shadow-[var(--shadow-md)] hover:border-[var(--color-border-strong)] cursor-pointer"
    : "";
  return <div className={`${base} ${hover} ${className}`} {...rest} />;
}
