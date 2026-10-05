package com.uberfreight.search.model;

/**
 * Validated search/filter parameters for GET /shipments/search.
 */
public record ShipmentSearchParams(
        String q,
        String origin,
        String destination,
        String equipmentType,
        Double minRate,
        Double maxRate,
        int page,
        int pageSize) {
}
