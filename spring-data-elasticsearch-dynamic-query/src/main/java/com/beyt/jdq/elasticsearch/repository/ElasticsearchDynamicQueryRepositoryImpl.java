package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.util.ListConsumer;
import com.beyt.jdq.core.model.builder.BaseQueryBuilder;
import com.beyt.jdq.elasticsearch.builder.ElasticsearchQueryBuilder;
import com.beyt.jdq.elasticsearch.core.ElasticsearchSearchQueryTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.SimpleElasticsearchRepository;

import java.io.Serializable;
import java.util.List;

/**
 * Elasticsearch implementation of ElasticsearchDynamicQueryRepository.
 * Provides dynamic query functionality for Elasticsearch.
 */
public class ElasticsearchDynamicQueryRepositoryImpl<T, ID extends Serializable> extends SimpleElasticsearchRepository<T, ID>
        implements ElasticsearchDynamicQueryRepository<T, ID> {

    private final ElasticsearchSearchQueryTemplate elasticsearchSearchQueryTemplate;
    private final Class<T> entityClass;
    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchDynamicQueryRepositoryImpl(
            Class<T> entityClass,
            ElasticsearchEntityInformation<T, ID> metadata,
            ElasticsearchOperations elasticsearchOperations,
            ElasticsearchSearchQueryTemplate elasticsearchSearchQueryTemplate) {
        super(metadata, elasticsearchOperations);
        this.elasticsearchSearchQueryTemplate = elasticsearchSearchQueryTemplate;
        this.entityClass = entityClass;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public ElasticsearchSearchQueryTemplate getElasticsearchSearchQueryTemplate() {
        return elasticsearchSearchQueryTemplate;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public List<T> findAll(List<Criteria> criteriaList) {
        return elasticsearchSearchQueryTemplate.findAll(entityClass, criteriaList);
    }

    @Override
    public List<T> findAll(DynamicQuery dynamicQuery) {
        return elasticsearchSearchQueryTemplate.findAll(entityClass, dynamicQuery);
    }

    @Override
    public <R> List<R> findAll(DynamicQuery dynamicQuery, Class<R> resultClass) {
        return elasticsearchSearchQueryTemplate.findAll(entityClass, dynamicQuery, resultClass);
    }

    @Override
    public Page<T> findAllAsPage(DynamicQuery dynamicQuery) {
        return elasticsearchSearchQueryTemplate.findAllAsPage(entityClass, dynamicQuery);
    }

    @Override
    public <R> Page<R> findAllAsPage(DynamicQuery dynamicQuery, Class<R> resultClass) {
        return elasticsearchSearchQueryTemplate.findAllAsPage(entityClass, dynamicQuery, resultClass);
    }

    @Override
    public Page<T> findAll(List<Criteria> criteriaList, Pageable pageable) {
        return elasticsearchSearchQueryTemplate.findAllAsPage(entityClass, criteriaList, pageable);
    }

    @Override
    public long count(List<Criteria> criteriaList) {
        return elasticsearchSearchQueryTemplate.count(entityClass, criteriaList);
    }

    @Override
    public void consumePartially(ListConsumer<T> processor, int pageSize) {
        consumePartially((List<Criteria>) null, processor, pageSize);
    }

    @Override
    public void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize) {
        long totalElements = criteriaList != null ?
            elasticsearchSearchQueryTemplate.count(entityClass, criteriaList) :
            count();

        for (int i = 0; (long) i * pageSize < totalElements; i++) {
            Page<T> page;
            if (criteriaList != null) {
                page = elasticsearchSearchQueryTemplate.findAllAsPage(
                    entityClass,
                    criteriaList,
                    PageRequest.of(i, pageSize)
                );
            } else {
                page = findAll(PageRequest.of(i, pageSize));
            }

            if (!page.getContent().isEmpty()) {
                processor.accept(page.getContent());
            }
        }
    }

    @Override
    public void consumePartially(DynamicQuery dynamicQuery, ListConsumer<T> processor, int pageSize) {
        long totalElements = elasticsearchSearchQueryTemplate.count(entityClass, dynamicQuery.getWhere());

        for (int i = 0; (long) i * pageSize < totalElements; i++) {
            dynamicQuery.setPageNumber(i);
            dynamicQuery.setPageSize(pageSize);

            Page<T> page = elasticsearchSearchQueryTemplate.findAllAsPage(entityClass, dynamicQuery);
            if (!page.getContent().isEmpty()) {
                processor.accept(page.getContent());
            }
        }
    }

    @Override
    public ElasticsearchQueryBuilder<T, ID> queryBuilder() {
        // Return a simple implementation that delegates to the repository methods
        return new ElasticsearchQueryBuilder<>(this);
    }
}

