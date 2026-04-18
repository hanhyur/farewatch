import { apiRequest } from "./client";
import type {
  FareSnapshot,
  FareStatistics,
  Judgment,
  Route,
} from "@/types/api";

export function fetchRoutes(): Promise<Route[]> {
  return apiRequest<Route[]>("/api/v1/routes");
}

export function fetchRoute(id: number): Promise<Route> {
  return apiRequest<Route>(`/api/v1/routes/${id}`);
}

export function fetchJudgment(
  id: number,
  departureDate?: string,
): Promise<Judgment> {
  return apiRequest<Judgment>(`/api/v1/routes/${id}/judgment`, {
    query: { departureDate },
  });
}

export function fetchFares(
  id: number,
  departureDate?: string,
  options?: { days?: number; limit?: number },
): Promise<FareSnapshot[]> {
  return apiRequest<FareSnapshot[]>(`/api/v1/routes/${id}/fares`, {
    query: {
      departureDate,
      days: options?.days,
      limit: options?.days ? undefined : (options?.limit ?? 30),
    },
  });
}

export function fetchStatistics(
  id: number,
  departureDate?: string,
): Promise<FareStatistics> {
  return apiRequest<FareStatistics>(`/api/v1/routes/${id}/statistics`, {
    query: { departureDate },
  });
}

export function createRoute(input: {
  origin: string;
  destination: string;
  airlineCode: string | null;
}): Promise<Route> {
  return apiRequest<Route>("/api/v1/routes", {
    method: "POST",
    body: input,
  });
}
