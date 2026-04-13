import { apiRequest } from "./client";
import type { FlightSearchResult, TripType } from "@/types/api";

export interface SearchParams {
  origin: string;
  destination: string;
  departureDate: string;
  returnDate?: string;
  tripType?: TripType;
  stops?: number;
}

export function searchFlights(params: SearchParams): Promise<FlightSearchResult> {
  return apiRequest<FlightSearchResult>("/api/v1/search", {
    query: {
      origin: params.origin,
      destination: params.destination,
      departureDate: params.departureDate,
      returnDate: params.returnDate,
      tripType: params.tripType ?? "ONE_WAY",
      stops: params.stops ?? 0,
    },
  });
}
