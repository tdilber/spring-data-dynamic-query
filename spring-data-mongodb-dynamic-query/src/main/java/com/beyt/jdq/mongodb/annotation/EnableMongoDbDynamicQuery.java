package com.beyt.jdq.mongodb.annotation;

import com.beyt.jdq.core.annotation.EnableDynamicQueryArgumentResolvers;
import com.beyt.jdq.core.config.DeserializerConfig;
import com.beyt.jdq.mongodb.config.MongoSearchQueryTemplateConfig;
import com.beyt.jdq.mongodb.repository.MongoDynamicQueryRepositoryFactoryBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable Dynamic Query support for MongoDB repositories.
 * 
 * Usage:
 * <pre>{@code
 * @Configuration
 * @EnableMongoDbDynamicQuery
 * public class MongoConfig {
 *     // Your MongoDB configuration
 * }
 * }</pre>
 * 
 * Then your repositories can extend MongoDynamicQueryRepository:
 * <pre>{@code
 * public interface UserRepository extends MongoDynamicQueryRepository<User, String> {
 *     // Your custom methods
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import({DeserializerConfig.class, MongoSearchQueryTemplateConfig.class})
@EnableMongoRepositories(repositoryFactoryBeanClass = MongoDynamicQueryRepositoryFactoryBean.class)
@EnableDynamicQueryArgumentResolvers
public @interface EnableMongoDbDynamicQuery {
}

