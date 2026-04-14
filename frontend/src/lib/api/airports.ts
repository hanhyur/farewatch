import { apiRequest } from "./client";
import type { AirportInfo } from "@/types/api";

export function searchAirports(q: string): Promise<AirportInfo[]> {
  return apiRequest<AirportInfo[]>("/api/v1/airports", { query: { q } });
}
