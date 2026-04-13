"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { AirportPicker } from "@/components/ui/AirportPicker";
import { searchFlights, type SearchParams } from "@/lib/api/search";
import { createRoute } from "@/lib/api/routes";
import { formatKrw } from "@/lib/format";
import type { AirportInfo, TripType } from "@/types/api";

function todayPlusDays(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return d.toISOString().split("T")[0];
}

export default function SearchPage() {
  const router = useRouter();

  const [origin, setOrigin] = useState<AirportInfo | null>(null);
  const [destination, setDestination] = useState<AirportInfo | null>(null);
  const [departureDate, setDepartureDate] = useState(todayPlusDays(7));
  const [returnDate, setReturnDate] = useState(todayPlusDays(14));
  const [tripType, setTripType] = useState<TripType>("ROUND_TRIP");
  const [stops, setStops] = useState(0);

  const [searchParams, setSearchParams] = useState<SearchParams | null>(null);
  const [registering, setRegistering] = useState<number | null>(null);

  const {
    data: result,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["search", searchParams],
    queryFn: () => searchFlights(searchParams!),
    enabled: searchParams !== null,
  });

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!origin || !destination || !departureDate) return;
    if (tripType === "ROUND_TRIP" && !returnDate) return;

    setSearchParams({
      origin: origin.iata,
      destination: destination.iata,
      departureDate,
      returnDate: tripType === "ROUND_TRIP" ? returnDate : undefined,
      tripType,
      stops,
    });
  }

  async function handleRegister(idx: number) {
    if (!searchParams) return;
    setRegistering(idx);
    try {
      const route = await createRoute({
        origin: searchParams.origin,
        destination: searchParams.destination,
        airlineCode: null,
      });
      router.push(`/routes/${route.id}`);
    } catch (err) {
      if (err instanceof Error && err.message.includes("already exists")) {
        router.push("/");
      }
    } finally {
      setRegistering(null);
    }
  }

  const priceLevelLabel: Record<string, string> = {
    low: "낮음",
    typical: "보통",
    high: "높음",
  };

  const stopsOptions = [
    { value: 0, label: "전체" },
    { value: 1, label: "직항만" },
    { value: 2, label: "경유 1회 이하" },
    { value: 3, label: "경유 2회 이하" },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
          항공편 검색
        </h1>
        <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
          도시명이나 공항 코드를 입력하면 실시간 항공편 가격을 확인할 수 있어요.
        </p>
      </div>

      {/* Search form */}
      <Card className="p-5">
        <form onSubmit={handleSearch} className="flex flex-col gap-4">
          {/* Trip type */}
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setTripType("ROUND_TRIP")}
              className={`rounded-full px-4 py-1.5 text-sm font-medium transition ${
                tripType === "ROUND_TRIP"
                  ? "bg-[var(--color-primary)] text-white"
                  : "bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] hover:bg-[var(--color-border)]"
              }`}
            >
              왕복
            </button>
            <button
              type="button"
              onClick={() => setTripType("ONE_WAY")}
              className={`rounded-full px-4 py-1.5 text-sm font-medium transition ${
                tripType === "ONE_WAY"
                  ? "bg-[var(--color-primary)] text-white"
                  : "bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] hover:bg-[var(--color-border)]"
              }`}
            >
              편도
            </button>
          </div>

          {/* Origin / Destination */}
          <div className="grid gap-4 sm:grid-cols-2">
            <AirportPicker
              id="origin"
              label="출발지"
              value={origin}
              onChange={setOrigin}
              placeholder="도시 또는 공항 검색"
            />
            <AirportPicker
              id="destination"
              label="도착지"
              value={destination}
              onChange={setDestination}
              placeholder="도시 또는 공항 검색"
            />
          </div>

          {/* Dates + Stops + Button */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label
                htmlFor="departureDate"
                className="mb-1.5 block text-sm font-medium text-[var(--color-text-secondary)]"
              >
                출국일
              </label>
              <input
                id="departureDate"
                type="date"
                value={departureDate}
                onChange={(e) => setDepartureDate(e.target.value)}
                min={todayPlusDays(0)}
                className="form-input"
                required
              />
            </div>

            {tripType === "ROUND_TRIP" && (
              <div>
                <label
                  htmlFor="returnDate"
                  className="mb-1.5 block text-sm font-medium text-[var(--color-text-secondary)]"
                >
                  귀국일
                </label>
                <input
                  id="returnDate"
                  type="date"
                  value={returnDate}
                  onChange={(e) => setReturnDate(e.target.value)}
                  min={departureDate || todayPlusDays(0)}
                  className="form-input"
                  required
                />
              </div>
            )}

            <div>
              <label
                htmlFor="stops"
                className="mb-1.5 block text-sm font-medium text-[var(--color-text-secondary)]"
              >
                경유
              </label>
              <select
                id="stops"
                value={stops}
                onChange={(e) => setStops(Number(e.target.value))}
                className="form-input"
              >
                {stopsOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-end">
              <Button
                type="submit"
                className="w-full"
                disabled={!origin || !destination}
              >
                검색하기
              </Button>
            </div>
          </div>
        </form>
      </Card>

      {/* Results */}
      {isLoading && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Card key={i} className="p-4">
              <div className="flex items-center gap-4">
                <Skeleton className="h-10 w-10 rounded-full" />
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-5 w-40" />
                  <Skeleton className="h-4 w-60" />
                </div>
                <Skeleton className="h-8 w-28" />
              </div>
            </Card>
          ))}
        </div>
      )}

      {isError && (
        <Card className="p-6 text-center">
          <p className="text-[var(--color-danger)] font-semibold">
            검색에 실패했어요
          </p>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            {error instanceof Error ? error.message : "알 수 없는 오류"}
          </p>
        </Card>
      )}

      {result && (
        <div className="flex flex-col gap-4">
          {/* Summary */}
          <div className="flex flex-wrap items-center gap-2 text-sm text-[var(--color-text-secondary)]">
            <span className="font-semibold text-[var(--color-text)]">
              {result.origin} → {result.destination}
            </span>
            <span>·</span>
            <span>{result.tripType === "ROUND_TRIP" ? "왕복" : "편도"}</span>
            <span>·</span>
            <span>{result.departureDate}</span>
            {result.returnDate && (
              <>
                <span>~</span>
                <span>{result.returnDate}</span>
              </>
            )}
            <span>·</span>
            <span>{result.flights.length}개 항공편</span>
          </div>

          {/* Price insights */}
          {result.priceInsights && (
            <Card className="flex flex-wrap items-center gap-x-8 gap-y-2 p-4 text-sm">
              <div>
                <span className="text-[var(--color-text-tertiary)]">최저가 </span>
                <span className="font-semibold tabular-nums">
                  {formatKrw(result.priceInsights.lowestPrice)}
                </span>
              </div>
              {result.priceInsights.typicalPriceLow &&
                result.priceInsights.typicalPriceHigh && (
                  <div>
                    <span className="text-[var(--color-text-tertiary)]">적정 범위 </span>
                    <span className="font-semibold tabular-nums">
                      {formatKrw(result.priceInsights.typicalPriceLow)} ~{" "}
                      {formatKrw(result.priceInsights.typicalPriceHigh)}
                    </span>
                  </div>
                )}
              {result.priceInsights.priceLevel && (
                <div className="flex items-center gap-1.5">
                  <span className="text-[var(--color-text-tertiary)]">가격 수준</span>
                  <span
                    className={`inline-flex h-6 items-center rounded-full px-2.5 text-xs font-semibold ${
                      result.priceInsights.priceLevel === "low"
                        ? "bg-[var(--color-success-soft)] text-[var(--color-success)]"
                        : result.priceInsights.priceLevel === "high"
                          ? "bg-[var(--color-danger-soft)] text-[var(--color-danger)]"
                          : "bg-[var(--color-warning-soft)] text-[var(--color-warning)]"
                    }`}
                  >
                    {priceLevelLabel[result.priceInsights.priceLevel] ??
                      result.priceInsights.priceLevel}
                  </span>
                </div>
              )}
            </Card>
          )}

          {/* Flights */}
          {result.flights.length === 0 ? (
            <Card className="p-6 text-center">
              <p className="font-semibold">검색 결과가 없어요</p>
              <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
                다른 날짜나 노선으로 검색해보세요.
              </p>
            </Card>
          ) : (
            <div className="flex flex-col gap-2">
              {result.flights.map((flight, idx) => (
                <Card key={idx} className="p-4">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-[var(--color-surface-muted)] text-[10px] font-bold text-[var(--color-text-secondary)]">
                        {flight.airlineCode || "--"}
                      </div>
                      <div>
                        <p className="text-sm font-semibold">{flight.airline}</p>
                        <p className="text-xs text-[var(--color-text-secondary)]">
                          {flight.departureTime && flight.arrivalTime
                            ? `${flight.departureTime} → ${flight.arrivalTime}`
                            : ""}
                          {flight.duration && (
                            <span className="text-[var(--color-text-tertiary)]">
                              {" "}· {flight.duration}
                            </span>
                          )}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <div className="flex items-center gap-1.5">
                        {flight.stops === 0 ? (
                          <span className="rounded-full bg-[var(--color-success-soft)] px-2 py-0.5 text-[10px] font-medium text-[var(--color-success)]">
                            직항
                          </span>
                        ) : (
                          <span className="rounded-full bg-[var(--color-warning-soft)] px-2 py-0.5 text-[10px] font-medium text-[var(--color-warning)]">
                            경유 {flight.stops}회
                          </span>
                        )}
                        {flight.isBest && (
                          <span className="rounded-full bg-[var(--color-primary-soft)] px-2 py-0.5 text-[10px] font-medium text-[var(--color-primary)]">
                            추천
                          </span>
                        )}
                      </div>

                      <p className="min-w-[90px] text-right text-lg font-bold tabular-nums">
                        {formatKrw(flight.price)}
                      </p>

                      <Button
                        variant="secondary"
                        size="md"
                        onClick={() => handleRegister(idx)}
                        disabled={registering !== null}
                        className="whitespace-nowrap text-xs"
                      >
                        {registering === idx ? "등록 중..." : "모니터링"}
                      </Button>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
