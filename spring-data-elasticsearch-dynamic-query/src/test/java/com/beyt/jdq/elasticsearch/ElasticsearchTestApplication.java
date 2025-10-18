package com.beyt.jdq.elasticsearch;

import com.beyt.jdq.elasticsearch.annotation.EnableElasticsearchDynamicQuery;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Test application for Elasticsearch dynamic query tests.
 * Excludes JPA auto-configuration since we're using Elasticsearch.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableElasticsearchDynamicQuery
public class ElasticsearchTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchTestApplication.class, args);
    }
}

