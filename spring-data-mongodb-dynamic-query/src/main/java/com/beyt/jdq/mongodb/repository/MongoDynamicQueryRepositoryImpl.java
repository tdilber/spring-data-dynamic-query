package com.beyt.jdq.mongodb.repository;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.util.ListConsumer;
import com.beyt.jdq.mongodb.builder.MongoQueryBuilder;
import com.beyt.jdq.mongodb.core.MongoSearchQueryTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;

import java.io.Serializable;
import java.util.List;

/**
 * MongoDB implementation of MongoDynamicQueryRepository.
 * Extends SimpleMongoRepository and provides dynamic query functionality.
 * 
 * @param <T> the domain type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 */
public class MongoDynamicQueryRepositoryImpl<T, ID extends Serializable>
        extends SimpleMongoRepository<T, ID> 
        implements MongoDynamicQueryRepository<T, ID> {

    private final MongoSearchQueryTemplate mongoSearchQueryTemplate;
    private final Class<T> entityClass;

    /**
     * Creates a new repository instance.
     * 
     * @param metadata the entity metadata
     * @param mongoOperations the MongoDB operations
     * @param mongoSearchQueryTemplate the search query template
     */
    public MongoDynamicQueryRepositoryImpl(
            MongoEntityInformation<T, ID> metadata,
            MongoOperations mongoOperations,
            MongoSearchQueryTemplate mongoSearchQueryTemplate) {
        super(metadata, mongoOperations);
        this.mongoSearchQueryTemplate = mongoSearchQueryTemplate;
        this.entityClass = metadata.getJavaType();
    }

    /**
     * Gets the MongoDB search query template.
     * 
     * @return the search query template
     */
    public MongoSearchQueryTemplate getMongoSearchQueryTemplate() {
        return mongoSearchQueryTemplate;
    }

    /**
     * Gets the entity class.
     * 
     * @return the entity class
     */
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public List<T> findAll(List<Criteria> criteriaList) {
        return mongoSearchQueryTemplate.findAll(entityClass, criteriaList);
    }

    @Override
    public List<T> findAll(DynamicQuery dynamicQuery) {
        return mongoSearchQueryTemplate.findAll(entityClass, dynamicQuery);
    }

    @Override
    public <R> List<R> findAll(DynamicQuery dynamicQuery, Class<R> resultClass) {
        return mongoSearchQueryTemplate.findAll(getEntityClass(), dynamicQuery, resultClass);
    }

    @Override
    public Page<T> findAllAsPage(DynamicQuery dynamicQuery) {
        return mongoSearchQueryTemplate.findAllAsPage(entityClass, dynamicQuery);
    }

    @Override
    public <R> Page<R> findAllAsPage(DynamicQuery dynamicQuery, Class<R> resultClass) {
        return mongoSearchQueryTemplate.findAllAsPage(getEntityClass(), dynamicQuery, resultClass);
    }

    @Override
    public Page<T> findAll(List<Criteria> criteriaList, Pageable pageable) {
        return mongoSearchQueryTemplate.findAllAsPage(entityClass, criteriaList, pageable);
    }

    @Override
    public long count(List<Criteria> criteriaList) {
        return mongoSearchQueryTemplate.count(entityClass, criteriaList);
    }

    @Override
    public void consumePartially(ListConsumer<T> processor, int pageSize) {
        consumePartially((List<Criteria>) null, processor, pageSize);
    }

    @Override
    public void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize) {
        long totalElements = criteriaList != null ? 
            mongoSearchQueryTemplate.count(entityClass, criteriaList) : 
            count();

        for (int i = 0; (long) i * pageSize < totalElements; i++) {
            Page<T> page;
            if (criteriaList != null) {
                page = mongoSearchQueryTemplate.findAllAsPage(
                    entityClass, 
                    criteriaList, 
                    PageRequest.of(i, pageSize)
                );
            } else {
                page = findAll(PageRequest.of(i, pageSize));
            }
            processor.accept(page.getContent());
        }
    }

    @Override
    public void consumePartially(DynamicQuery dynamicQuery, ListConsumer<T> processor, int pageSize) {
        dynamicQuery.setPageSize(pageSize);
        long totalElements = mongoSearchQueryTemplate.count(entityClass, dynamicQuery.getWhere());

        for (int i = 0; (long) i * pageSize < totalElements; i++) {
            dynamicQuery.setPageNumber(i);
            List<T> content = mongoSearchQueryTemplate.findAll(entityClass, dynamicQuery);
            processor.accept(content);
        }
    }

    @Override
    public MongoQueryBuilder<T, ID> queryBuilder() {
        return new MongoQueryBuilder<>(this);
    }
}

