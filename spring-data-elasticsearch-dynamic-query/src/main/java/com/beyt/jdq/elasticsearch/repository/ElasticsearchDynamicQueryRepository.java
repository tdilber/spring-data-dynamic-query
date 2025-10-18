package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.model.builder.BaseQueryBuilder;
import com.beyt.jdq.core.repository.BaseDynamicQueryRepository;
import com.beyt.jdq.core.util.ListConsumer;
import com.beyt.jdq.elasticsearch.builder.ElasticsearchQueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Elasticsearch extension of ElasticsearchRepository with dynamic query capabilities.
 * Provides methods similar to DynamicQueryRepository but for Elasticsearch.
 * 
 * To use this interface, you need to:
 * 1. Extend this interface for your entity repository
 * 2. Provide an ElasticsearchSearchQueryTemplate bean in your configuration
 * 
 * Note: Unlike JPA version, this interface uses methods that require
 * an ElasticsearchSearchQueryTemplate to be injected via a custom implementation.
 */
@NoRepositoryBean
public interface ElasticsearchDynamicQueryRepository<T, ID> extends BaseDynamicQueryRepository<T, ID>, ElasticsearchRepository<T, ID> {

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
     */
    void consumePartially(DynamicQuery dynamicQuery, ListConsumer<T> processor, int pageSize);

    // Note: queryBuilder() method is inherited from BaseDynamicQueryRepository
    BaseQueryBuilder<T, ID> queryBuilder();
}

