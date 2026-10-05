package com.uberfreight.search.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ShipmentSearchResponse(
        @JsonProperty("items") List<ShipmentSummary> items,
        @JsonProperty("total") long total,
        @JsonProperty("page") int page,
        @JsonProperty("page_size") int pageSize) {
}
