export interface Shipment {
  id: string;
  origin_city: string;
  origin_state: string;
  destination_city: string;
  destination_state: string;
  pickup_date: string;
  equipment_type: string;
  rate_usd: number;
  weight_lbs: number;
  status: string;
  updated_at: string;
}

export interface ShipmentSearchResponse {
  items: Shipment[];
  total: number;
  page: number;
  page_size: number;
}

export interface SearchFilters {
  q: string;
  origin: string;
  destination: string;
  equipment_type: string;
  min_rate: string;
  max_rate: string;
}
