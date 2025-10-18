package com.beyt.jdq.elasticsearch.config;

import com.beyt.jdq.core.deserializer.IDeserializer;
import com.beyt.jdq.elasticsearch.core.ElasticsearchSearchQueryTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Configuration class for ElasticsearchSearchQueryTemplate bean.
 * Auto-configures the template with ElasticsearchOperations and IDeserializer.
 */
@Configuration
public class ElasticsearchSearchQueryTemplateConfig {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private IDeserializer deserializer;

    /**
     * Creates and configures the ElasticsearchSearchQueryTemplate bean.
     * 
     * @return configured ElasticsearchSearchQueryTemplate instance
     */
    @Bean
    public ElasticsearchSearchQueryTemplate elasticsearchSearchQueryTemplate() {
        return new ElasticsearchSearchQueryTemplate(elasticsearchOperations, deserializer);
    }
}




