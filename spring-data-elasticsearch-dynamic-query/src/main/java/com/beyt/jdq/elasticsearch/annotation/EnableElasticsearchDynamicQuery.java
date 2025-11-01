package com.beyt.jdq.elasticsearch.annotation;

import com.beyt.jdq.core.config.DeserializerConfig;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchDynamicQueryRepositoryFactoryBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable Dynamic Query support for Elasticsearch repositories.
 * 
 * Usage:
 * <pre>{@code
 * @Configuration
 * @EnableElasticsearchDynamicQuery
 * public class ElasticsearchConfig {
 *     // Your Elasticsearch configuration
 * }
 * }</pre>
 * 
 * Then your repositories can extend ElasticsearchDynamicQueryRepository:
 * <pre>{@code
 * public interface UserRepository extends ElasticsearchDynamicQueryRepository<User, String> {
 *     // Your custom methods
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import({DeserializerConfig.class})
@EnableElasticsearchRepositories(repositoryFactoryBeanClass = ElasticsearchDynamicQueryRepositoryFactoryBean.class)
public @interface EnableElasticsearchDynamicQuery {
}




