package com.uberfreight.search.api;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reports whether the API, PostgreSQL, and Elasticsearch are reachable.
 * No credentials or connection details are included in the response.
 */
@RestController
public class HealthController {

    private final DataSource dataSource;
    private final ElasticsearchClient elasticsearchClient;

    public HealthController(DataSource dataSource, ElasticsearchClient elasticsearchClient) {
        this.dataSource = dataSource;
        this.elasticsearchClient = elasticsearchClient;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        boolean postgresUp = isPostgresReachable();
        boolean elasticsearchUp = isElasticsearchReachable();

        return Map.of(
                "status", postgresUp && elasticsearchUp ? "ok" : "degraded",
                "postgres", postgresUp,
                "elasticsearch", elasticsearchUp);
    }

    private boolean isPostgresReachable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isElasticsearchReachable() {
        try {
            return elasticsearchClient.ping().value();
        } catch (Exception e) {
            return false;
        }
    }
}
