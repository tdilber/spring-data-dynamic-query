package com.beyt.jdq.mongo;

import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.DynamicQuery;
import com.beyt.jdq.util.ListConsumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * MongoDB extension of MongoRepository with dynamic query capabilities.
 * Provides methods similar to JpaDynamicQueryRepository but for MongoDB.
 * 
 * To use this interface, you need to:
 * 1. Extend this interface for your entity repository
 * 2. Provide a MongoSearchQueryTemplate bean in your configuration
 * 
 * Note: Unlike JPA version, this interface uses default methods that require 
 * a MongoSearchQueryTemplate to be injected via a custom implementation.
 */
@NoRepositoryBean
public interface JpaDynamicQueryMongoRepository<T, ID> extends MongoRepository<T, ID> {

    /**
     * Get the MongoSearchQueryTemplate instance.
     * This should be implemented by the repository implementation class.
     */
    MongoSearchQueryTemplate getMongoSearchQueryTemplate();

    /**
     * Get the entity class for this repository.
     * This should be implemented by the repository implementation class.
     */
    Class<T> getEntityClass();

    /**
     * Find all entities matching the criteria list
     */
    default List<T> findAll(List<Criteria> criteriaList) {
        return getMongoSearchQueryTemplate().findAll(getEntityClass(), criteriaList);
    }

    /**
     * Find all entities matching the dynamic query
     */
    default List<T> findAll(DynamicQuery dynamicQuery) {
        return getMongoSearchQueryTemplate().findAll(getEntityClass(), dynamicQuery);
    }

    /**
     * Find all entities with projection matching the dynamic query
     */
    default <R> List<R> findAll(DynamicQuery dynamicQuery, Class<R> resultClass) {
        return getMongoSearchQueryTemplate().findAll(getEntityClass(), dynamicQuery, resultClass);
    }

    /**
     * Find all entities as page matching the dynamic query
     */
    default Page<T> findAllAsPage(DynamicQuery dynamicQuery) {
        return getMongoSearchQueryTemplate().findAllAsPage(getEntityClass(), dynamicQuery);
    }

    /**
     * Find all entities as page with projection matching the dynamic query
     */
    default <R> Page<R> findAllAsPage(DynamicQuery dynamicQuery, Class<R> resultClass) {
        return getMongoSearchQueryTemplate().findAllAsPage(getEntityClass(), dynamicQuery, resultClass);
    }

    /**
     * Find all entities as page matching the criteria list
     */
    default Page<T> findAll(List<Criteria> criteriaList, Pageable pageable) {
        return getMongoSearchQueryTemplate().findAllAsPage(getEntityClass(), criteriaList, pageable);
    }

    /**
     * Count entities matching the criteria list
     */
    default long count(List<Criteria> criteriaList) {
        return getMongoSearchQueryTemplate().count(getEntityClass(), criteriaList);
    }

    /**
     * Consume all entities matching no filter in batches
     */
    default void consumePartially(ListConsumer<T> processor, int pageSize) {
        consumePartially((List<Criteria>) null, processor, pageSize);
    }

    /**
     * Consume all entities matching criteria list in batches
     */
    default void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize) {
        long totalElements = criteriaList != null ? 
            getMongoSearchQueryTemplate().count(getEntityClass(), criteriaList) : 
            count();

        for (int i = 0; (long) i * pageSize < totalElements; i++) {
            Page<T> page;
            if (criteriaList != null) {
                page = getMongoSearchQueryTemplate().findAllAsPage(
                    getEntityClass(), 
                    criteriaList, 
                    PageRequest.of(i, pageSize)
                );
            } else {
                page = findAll(PageRequest.of(i, pageSize));
            }
            processor.accept(page.getContent());
        }
    }

    /**
     * Consume all entities matching dynamic query in batches
     */
    default void consumePartially(DynamicQuery dynamicQuery, ListConsumer<T> processor, int pageSize) {
        dynamicQuery.setPageSize(pageSize);
        long totalElements = getMongoSearchQueryTemplate().count(getEntityClass(), dynamicQuery.getWhere());

        for (int i = 0; (long) i * pageSize < totalElements; i++) {
            dynamicQuery.setPageNumber(i);
            List<T> content = getMongoSearchQueryTemplate().findAll(getEntityClass(), dynamicQuery);
            processor.accept(content);
        }
    }
}
