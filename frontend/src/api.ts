import type { SearchFilters, ShipmentSearchResponse } from "./types";

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function searchShipments(
  filters: SearchFilters,
  page: number,
  pageSize: number,
): Promise<ShipmentSearchResponse> {
  const params = new URLSearchParams();
  if (filters.q) params.set("q", filters.q);
  if (filters.origin) params.set("origin", filters.origin);
  if (filters.destination) params.set("destination", filters.destination);
  if (filters.equipment_type) params.set("equipment_type", filters.equipment_type);
  if (filters.min_rate) params.set("min_rate", filters.min_rate);
  if (filters.max_rate) params.set("max_rate", filters.max_rate);
  params.set("page", String(page));
  params.set("page_size", String(pageSize));

  const response = await fetch(`${API_URL}/shipments/search?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`Search failed (${response.status})`);
  }
  return response.json();
}
