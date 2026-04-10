import { apiRequest } from "./client";
import type { Notification } from "@/types/api";

export function fetchNotifications(limit = 50): Promise<Notification[]> {
  return apiRequest<Notification[]>("/api/v1/notifications", {
    query: { limit },
  });
}
