import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import "./globals.css";
import { QueryProvider } from "@/lib/query-provider";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "FareWatch — 지금 사도 되는 항공권인가요?",
  description:
    "가격 히스토리 기반으로 지금 항공권을 사도 되는지 알려주는 대시보드",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ko"
      className={`${geistSans.variable} ${geistMono.variable} h-full`}
    >
      <body className="min-h-full flex flex-col">
        <QueryProvider>
          <header className="sticky top-0 z-20 border-b border-[var(--color-border)] bg-[var(--color-surface)]/80 backdrop-blur">
            <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-5">
              <Link
                href="/"
                className="flex items-center gap-2 font-semibold tracking-tight"
              >
                <span className="inline-block h-7 w-7 rounded-lg bg-[var(--color-primary)]" />
                <span className="text-lg">FareWatch</span>
              </Link>
              <nav className="flex items-center gap-1 text-sm text-[var(--color-text-secondary)]">
                <Link
                  href="/"
                  className="rounded-full px-3 py-1.5 hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text)]"
                >
                  대시보드
                </Link>
                <Link
                  href="/notifications"
                  className="rounded-full px-3 py-1.5 hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text)]"
                >
                  알림 이력
                </Link>
              </nav>
            </div>
          </header>
          <main className="mx-auto w-full max-w-6xl flex-1 px-5 py-10">
            {children}
          </main>
          <footer className="border-t border-[var(--color-border)] py-6">
            <div className="mx-auto max-w-6xl px-5 text-xs text-[var(--color-text-tertiary)]">
              FareWatch · 가격 히스토리 기반 항공권 판단 대시보드
            </div>
          </footer>
        </QueryProvider>
      </body>
    </html>
  );
}
