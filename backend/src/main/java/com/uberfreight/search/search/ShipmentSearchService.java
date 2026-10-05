package com.uberfreight.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.uberfreight.search.model.ShipmentSearchParams;
import com.uberfreight.search.model.ShipmentSearchResponse;
import com.uberfreight.search.model.ShipmentSummary;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Executes shipment searches against Elasticsearch. The query shape itself
 * lives in ShipmentQueryBuilder; this class only wires params -> query -> response.
 */
@Service
public class ShipmentSearchService {

    private final ElasticsearchClient client;
    private final String index;

    public ShipmentSearchService(
            ElasticsearchClient client,
            @Value("${freight.elasticsearch.index}") String index) {
        this.client = client;
        this.index = index;
    }

    public ShipmentSearchResponse search(ShipmentSearchParams params) {
        int from = (params.page() - 1) * params.pageSize();

        try {
            SearchResponse<ShipmentSummary> response = client.search(s -> s
                    .index(index)
                    .query(ShipmentQueryBuilder.build(params))
                    .from(from)
                    .size(params.pageSize()),
                    ShipmentSummary.class);

            List<ShipmentSummary> items = response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .toList();

            long total = response.hits().total() != null ? response.hits().total().value() : items.size();

            return new ShipmentSearchResponse(items, total, params.page(), params.pageSize());
        } catch (IOException e) {
            throw new ShipmentSearchException("Failed to search shipments in Elasticsearch", e);
        }
    }
}
