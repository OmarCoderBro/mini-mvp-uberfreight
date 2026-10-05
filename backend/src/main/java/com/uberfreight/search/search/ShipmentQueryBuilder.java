package com.uberfreight.search.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.json.JsonData;
import com.uberfreight.search.model.ShipmentSearchParams;
import java.util.List;

/**
 * Builds the Elasticsearch query for a shipment search. Kept separate from the
 * service so the query shape can be tested without a live Elasticsearch instance.
 */
public final class ShipmentQueryBuilder {

    private static final List<String> FREE_TEXT_FIELDS =
            List.of("origin_city", "origin_state", "destination_city", "destination_state");

    private ShipmentQueryBuilder() {
    }

    public static Query build(ShipmentSearchParams params) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        boolean hasClause = false;

        if (hasText(params.q())) {
            bool.must(q -> q.multiMatch(m -> m
                    .query(params.q())
                    .fields(FREE_TEXT_FIELDS)
                    .type(TextQueryType.BoolPrefix)));
            hasClause = true;
        }

        if (hasText(params.origin())) {
            bool.filter(locationFilter(params.origin(), "origin_city", "origin_state"));
            hasClause = true;
        }

        if (hasText(params.destination())) {
            bool.filter(locationFilter(params.destination(), "destination_city", "destination_state"));
            hasClause = true;
        }

        if (hasText(params.equipmentType())) {
            bool.filter(q -> q.term(t -> t.field("equipment_type").value(params.equipmentType())));
            hasClause = true;
        }

        if (params.minRate() != null || params.maxRate() != null) {
            bool.filter(q -> q.range(r -> {
                r.field("rate_usd");
                if (params.minRate() != null) {
                    r.gte(JsonData.of(params.minRate()));
                }
                if (params.maxRate() != null) {
                    r.lte(JsonData.of(params.maxRate()));
                }
                return r;
            }));
            hasClause = true;
        }

        if (hasClause) {
            return Query.of(q -> q.bool(bool.build()));
        }
        return Query.of(q -> q.matchAll(m -> m));
    }

    private static Query locationFilter(String value, String cityField, String stateField) {
        String normalized = value.toLowerCase();
        return Query.of(q -> q.bool(b -> b
                .minimumShouldMatch("1")
                .should(s -> s.term(t -> t.field(cityField + ".keyword").value(normalized)))
                .should(s -> s.prefix(p -> p.field(cityField + ".keyword").value(normalized)))
                .should(s -> s.term(t -> t.field(stateField).value(normalized)))
                .should(s -> s.prefix(p -> p.field(stateField).value(normalized)))));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
