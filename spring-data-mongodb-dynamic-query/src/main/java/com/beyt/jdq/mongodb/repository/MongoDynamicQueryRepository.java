package com.beyt.jdq.mongodb.repository;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.repository.BaseDynamicQueryRepository;
import com.beyt.jdq.core.util.ListConsumer;
import com.beyt.jdq.mongodb.builder.MongoQueryBuilder;
import com.beyt.jdq.mongodb.core.MongoSearchQueryTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * MongoDB extension of MongoRepository with dynamic query capabilities.
 * Provides methods similar to DynamicQueryRepository but for MongoDB.
 * 
 * To use this interface, you need to:
 * 1. Extend this interface for your entity repository
 * 2. Provide a MongoSearchQueryTemplate bean in your configuration
 * 
 * Note: Unlike JPA version, this interface uses methods that require
 * a MongoSearchQueryTemplate to be injected via a custom implementation.
 * 
 * @param <T> the domain type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 */
@NoRepositoryBean
public interface MongoDynamicQueryRepository<T, ID> extends BaseDynamicQueryRepository<T, ID>, MongoRepository<T, ID> {

    /**
     * Find all entities matching the criteria list
     */
    List<T> findAll(List<Criteria> criteriaList);

    /**
     * Find all entities matching the dynamic query
     */
    List<T> findAll(DynamicQuery dynamicQuery);

    /**
     * Find all entities with projection matching the dynamic query
     */
    <R> List<R> findAll(DynamicQuery dynamicQuery, Class<R> resultClass);

    /**
     * Find all entities as page matching the dynamic query
     */
    Page<T> findAllAsPage(DynamicQuery dynamicQuery);

    /**
     * Find all entities as page with projection matching the dynamic query
     */
    <R> Page<R> findAllAsPage(DynamicQuery dynamicQuery, Class<R> resultClass);

    /**
     * Find all entities as page matching the criteria list
     */
    Page<T> findAll(List<Criteria> criteriaList, Pageable pageable);

    /**
     * Count entities matching the criteria list
     */
    long count(List<Criteria> criteriaList);

    /**
     * Consume all entities matching no filter in batches
     */
    void consumePartially(ListConsumer<T> processor, int pageSize);

    /**
     * Consume all entities matching criteria list in batches
     */
    void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize);

    /**
     * Consume all entities matching dynamic query in batches
     * 
     * @param dynamicQuery the dynamic query to filter entities
     * @param processor the consumer to process each batch
     * @param pageSize the size of each batch
     */
    void consumePartially(DynamicQuery dynamicQuery, ListConsumer<T> processor, int pageSize);

    /**
     * Create a query builder for fluent query construction
     */
    MongoQueryBuilder<T, ID> queryBuilder();
}
