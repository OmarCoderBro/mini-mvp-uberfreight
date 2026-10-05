package com.uberfreight.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient(@Value("${freight.elasticsearch.url}") String url) {
        return ElasticsearchClientFactory.create(url);
    }
}
