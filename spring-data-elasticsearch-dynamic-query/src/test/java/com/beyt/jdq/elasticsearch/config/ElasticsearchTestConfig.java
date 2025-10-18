package com.beyt.jdq.elasticsearch.config;

import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch configuration for tests.
 * Enables Elasticsearch repositories for the test package.
 */
@Configuration
public class ElasticsearchTestConfig {
    // ElasticsearchOperations bean will be auto-configured by Spring Boot
    // ElasticsearchSearchQueryTemplate will be configured by @EnableElasticsearchDynamicQuery
}

