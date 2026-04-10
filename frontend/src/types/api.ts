export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: string | null;
  timestamp: string;
}

export interface Route {
  id: number;
  origin: string;
  destination: string;
  airlineCode: string | null;
  active: boolean;
}

export type Verdict = "CHEAP" | "FAIR" | "EXPENSIVE" | "INSUFFICIENT";

export interface Judgment {
  routeId: number;
  departureDate: string;
  verdict: Verdict;
  currentPrice: number | null;
  avgPrice: number | null;
  minPrice: number | null;
  maxPrice: number | null;
  stdDeviation: number | null;
  zScore: number | null;
  sampleCount: number;
  suggestion: string;
  calculatedAt: string;
}

export interface FareSnapshot {
  id: number;
  routeId: number;
  departureDate: string;
  collectedAt: string;
  price: number;
  currency: string;
  source?: string;
}

export interface FareStatistics {
  routeId: number;
  departureDate: string;
  avgPrice: number;
  minPrice: number;
  maxPrice: number;
  stdDeviation: number;
  sampleCount: number;
  p25Price: number | null;
  p75Price: number | null;
  calculatedAt: string;
}

export type VerdictTrigger = "CHEAP" | "CHEAP_OR_FAIR";

export interface AlertRule {
  id: number;
  routeId: number;
  userIdentifier: string;
  departureDateFrom: string;
  departureDateTo: string;
  targetPrice: number | null;
  verdictTrigger: VerdictTrigger;
  active: boolean;
  createdAt: string;
}

export interface CreateAlertRuleInput {
  routeId: number;
  userIdentifier: string;
  departureDateFrom: string;
  departureDateTo: string;
  targetPrice?: number | null;
  verdictTrigger: VerdictTrigger;
}

export interface Notification {
  id: number;
  alertRuleId: number;
  routeId: number;
  departureDate: string;
  sentAt: string;
  verdict: Verdict;
  priceAtSend: number;
  channel: string;
  message: string;
}
