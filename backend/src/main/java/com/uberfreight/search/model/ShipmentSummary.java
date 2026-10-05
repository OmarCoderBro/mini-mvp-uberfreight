package com.uberfreight.search.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Shape of a shipment document as stored in Elasticsearch and returned by the API.
 * Field names are snake_case to match the API contract and the PostgreSQL columns.
 */
public record ShipmentSummary(
        @JsonProperty("id") String id,
        @JsonProperty("origin_city") String originCity,
        @JsonProperty("origin_state") String originState,
        @JsonProperty("destination_city") String destinationCity,
        @JsonProperty("destination_state") String destinationState,
        @JsonProperty("pickup_date") String pickupDate,
        @JsonProperty("equipment_type") String equipmentType,
        @JsonProperty("rate_usd") double rateUsd,
        @JsonProperty("weight_lbs") int weightLbs,
        @JsonProperty("status") String status,
        @JsonProperty("updated_at") String updatedAt) {
}
