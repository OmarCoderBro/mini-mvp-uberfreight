import { FormEvent, useEffect, useState } from "react";
import { searchShipments } from "./api";
import type { SearchFilters, Shipment } from "./types";

const PAGE_SIZE = 20;
const EQUIPMENT_TYPES = ["Dry Van", "Reefer", "Flatbed", "Step Deck", "Tanker"];

const EMPTY_FILTERS: SearchFilters = {
  q: "",
  origin: "",
  destination: "",
  equipment_type: "",
  min_rate: "",
  max_rate: "",
};

type Status = "idle" | "loading" | "error" | "loaded";

export default function App() {
  const [filters, setFilters] = useState<SearchFilters>(EMPTY_FILTERS);
  const [page, setPage] = useState(1);
  const [items, setItems] = useState<Shipment[]>([]);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState<Status>("idle");
  const [error, setError] = useState<string | null>(null);

  async function runSearch(currentFilters: SearchFilters, currentPage: number) {
    setStatus("loading");
    setError(null);
    try {
      const response = await searchShipments(currentFilters, currentPage, PAGE_SIZE);
      setItems(response.items);
      setTotal(response.total);
      setStatus("loaded");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
      setStatus("error");
    }
  }

  useEffect(() => {
    runSearch(EMPTY_FILTERS, 1);
  }, []);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setPage(1);
    runSearch(filters, 1);
  }

  function goToPage(nextPage: number) {
    setPage(nextPage);
    runSearch(filters, nextPage);
  }

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div className="app">
      <header>
        <h1>Freight Shipment Search</h1>
        <p className="synthetic-badge">Synthetic demo data — not real shipments or carrier information</p>
      </header>

      <form className="filters" onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Search city or state (e.g. Chicago)"
          value={filters.q}
          onChange={(e) => setFilters({ ...filters, q: e.target.value })}
          className="search-box"
        />
        <input
          type="text"
          placeholder="Origin"
          value={filters.origin}
          onChange={(e) => setFilters({ ...filters, origin: e.target.value })}
        />
        <input
          type="text"
          placeholder="Destination"
          value={filters.destination}
          onChange={(e) => setFilters({ ...filters, destination: e.target.value })}
        />
        <select
          value={filters.equipment_type}
          onChange={(e) => setFilters({ ...filters, equipment_type: e.target.value })}
        >
          <option value="">Any equipment</option>
          {EQUIPMENT_TYPES.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
        <input
          type="number"
          placeholder="Min rate ($)"
          value={filters.min_rate}
          onChange={(e) => setFilters({ ...filters, min_rate: e.target.value })}
        />
        <input
          type="number"
          placeholder="Max rate ($)"
          value={filters.max_rate}
          onChange={(e) => setFilters({ ...filters, max_rate: e.target.value })}
        />
        <button type="submit">Search</button>
      </form>

      <main>
        {status === "loading" && <p className="state-message">Loading shipments...</p>}
        {status === "error" && (
          <p className="state-message error">Couldn't load shipments: {error}</p>
        )}
        {status === "loaded" && items.length === 0 && (
          <p className="state-message">No shipments match your search.</p>
        )}
        {status === "loaded" && items.length > 0 && (
          <>
            <p className="results-count">
              {total} matching shipment{total === 1 ? "" : "s"} &middot; page {page} of {totalPages}
            </p>
            <table className="results">
              <thead>
                <tr>
                  <th>Origin</th>
                  <th>Destination</th>
                  <th>Pickup</th>
                  <th>Equipment</th>
                  <th>Rate</th>
                  <th>Weight</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {items.map((shipment) => (
                  <tr key={shipment.id}>
                    <td>{shipment.origin_city}, {shipment.origin_state}</td>
                    <td>{shipment.destination_city}, {shipment.destination_state}</td>
                    <td>{shipment.pickup_date}</td>
                    <td>{shipment.equipment_type}</td>
                    <td>${shipment.rate_usd.toLocaleString()}</td>
                    <td>{shipment.weight_lbs.toLocaleString()} lbs</td>
                    <td>{shipment.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="pagination">
              <button disabled={page <= 1} onClick={() => goToPage(page - 1)}>
                Previous
              </button>
              <button disabled={page >= totalPages} onClick={() => goToPage(page + 1)}>
                Next
              </button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
