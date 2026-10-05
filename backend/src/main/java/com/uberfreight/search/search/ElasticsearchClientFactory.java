package com.uberfreight.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

/**
 * Builds the Elasticsearch client. Shared by the Spring Boot API (as a bean)
 * and the standalone seed/reindex scripts (called directly, no Spring context).
 */
public final class ElasticsearchClientFactory {

    private ElasticsearchClientFactory() {
    }

    public static ElasticsearchClient create(String url) {
        HttpHost host = HttpHost.create(url);
        RestClient restClient = RestClient.builder(host).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    /**
     * Closes the client's underlying HTTP connections. ElasticsearchClient itself is
     * not Closeable; standalone scripts must call this when done, or the JVM process
     * never exits because the REST client keeps background threads alive.
     */
    public static void close(ElasticsearchClient client) throws IOException {
        client._transport().close();
    }
}
