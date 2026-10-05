package com.uberfreight.search.scripts;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.uberfreight.search.model.ShipmentSummary;
import com.uberfreight.search.search.ElasticsearchClientFactory;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Rebuilds the Elasticsearch "shipments" index from PostgreSQL.
 * Run with: mvn exec:java -Dexec.mainClass=com.uberfreight.search.scripts.ReindexRunner
 *
 * Uses the PostgreSQL id as the Elasticsearch document id, so running this
 * repeatedly overwrites existing documents instead of creating duplicates.
 */
public final class ReindexRunner {

    private static final int BATCH_SIZE = 200;

    public static void main(String[] args) throws Exception {
        String dbUrl = env("DB_URL", "jdbc:postgresql://localhost:5433/freight");
        String dbUser = env("DB_USER", "freight");
        String dbPassword = env("DB_PASSWORD", "freight");
        String esUrl = env("ES_URL", "http://localhost:9200");
        String esIndex = env("ES_INDEX", "shipments");

        int indexed = 0;
        int failed = 0;

        ElasticsearchClient client = ElasticsearchClientFactory.create(esUrl);
        try {
            ensureIndexExists(client, esIndex);

            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                List<ShipmentSummary> batch = new ArrayList<>(BATCH_SIZE);

                try (Statement statement = connection.createStatement();
                        ResultSet rs = statement.executeQuery("""
                                SELECT id, origin_city, origin_state, destination_city, destination_state,
                                       pickup_date, equipment_type, rate_usd, weight_lbs, status, updated_at
                                FROM shipments
                                ORDER BY id
                                """)) {
                    while (rs.next()) {
                        batch.add(new ShipmentSummary(
                                String.valueOf(rs.getLong("id")),
                                rs.getString("origin_city"),
                                rs.getString("origin_state"),
                                rs.getString("destination_city"),
                                rs.getString("destination_state"),
                                rs.getDate("pickup_date").toString(),
                                rs.getString("equipment_type"),
                                rs.getDouble("rate_usd"),
                                rs.getInt("weight_lbs"),
                                rs.getString("status"),
                                rs.getTimestamp("updated_at").toInstant().toString()));

                        if (batch.size() == BATCH_SIZE) {
                            failed += bulkUpsert(client, esIndex, batch);
                            indexed += batch.size();
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty()) {
                        failed += bulkUpsert(client, esIndex, batch);
                        indexed += batch.size();
                    }
                }
            }
        } finally {
            ElasticsearchClientFactory.close(client);
        }

        System.out.println("Reindexed " + indexed + " shipments (" + failed + " failed).");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void ensureIndexExists(ElasticsearchClient client, String index) throws Exception {
        boolean exists = client.indices().exists(e -> e.index(index)).value();
        if (exists) {
            return;
        }
        try (InputStream mapping = ReindexRunner.class.getResourceAsStream(
                "/elasticsearch/shipments-index.json")) {
            client.indices().create(c -> c.index(index).withJson(mapping));
        }
    }

    private static int bulkUpsert(ElasticsearchClient client, String index, List<ShipmentSummary> batch)
            throws Exception {
        BulkRequest.Builder bulk = new BulkRequest.Builder();
        for (ShipmentSummary shipment : batch) {
            bulk.operations(op -> op.index(idx -> idx
                    .index(index)
                    .id(shipment.id())
                    .document(shipment)));
        }

        BulkResponse response = client.bulk(bulk.build());
        int failures = 0;
        if (response.errors()) {
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    failures++;
                    System.err.println("Failed to index shipment " + item.id() + ": " + item.error().reason());
                }
            }
        }
        return failures;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
