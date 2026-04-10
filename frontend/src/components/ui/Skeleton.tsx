interface SkeletonProps {
  className?: string;
}

export function Skeleton({ className = "" }: SkeletonProps) {
  return (
    <div
      className={`animate-pulse rounded-[var(--radius-sm)] bg-[var(--color-surface-muted)] ${className}`}
    />
  );
}
