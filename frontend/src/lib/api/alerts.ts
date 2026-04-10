import { apiRequest } from "./client";
import type { AlertRule, CreateAlertRuleInput } from "@/types/api";

export function fetchAlertRules(routeId?: number): Promise<AlertRule[]> {
  return apiRequest<AlertRule[]>("/api/v1/alert-rules", {
    query: { routeId },
  });
}

export function createAlertRule(
  input: CreateAlertRuleInput,
): Promise<AlertRule> {
  return apiRequest<AlertRule>("/api/v1/alert-rules", {
    method: "POST",
    body: input,
  });
}

export function deleteAlertRule(id: number): Promise<void> {
  return apiRequest<void>(`/api/v1/alert-rules/${id}`, {
    method: "DELETE",
  });
}
